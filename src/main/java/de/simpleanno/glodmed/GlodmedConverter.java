package de.simpleanno.glodmed;

import de.simpleanno.glodmed.owl.GlodmedOwlOutputGenerator;
import de.simpleanno.glodmed.skos.GlodmedSkosOwlOutputGenerator;
import de.simpleanno.glodmed.skos.GlodmedSkosRdfOutputGenerator;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.simpleanno.glodmed.GlodmedConverter.CSVField.*;

public class GlodmedConverter {

    enum Mode {owl(GlodmedOwlOutputGenerator.class), skos_rdf(GlodmedSkosRdfOutputGenerator.class), skos_owl(GlodmedSkosOwlOutputGenerator.class);
        public Class generator;
        Mode(Class generator) {this.generator = generator;}
    };

    enum CSVField {id, lang, glossary, mainterm, subterm, definition, see, see_also, compare, figure_filename,
        figure_legend, figure_source, reference_literature, unique_test};

    private final Pattern markupPattern = Pattern.compile("\\[.*?\\]|<.*?>");

    private int currentId;
    private int lastId = -1;
    private GlodmedEntry currentEntry;

    private void transform(Mode mode, Reader in, File destination) throws Exception {

        CSVParser csvParser = CSVFormat.newFormat(',').withFirstRecordAsHeader().withQuote('"').parse(in);

        Glodmed glodmed = Glodmed.instance();

        // We need two passes: With the first pass we get all entries, with the second pass we add the references
        // (cannot be done in one pass because there can be references to objects that have not been created yet)

        // first pass

        csvParser.getRecords().stream().forEach(record -> {

            currentId = Integer.parseInt(record.get(id));

            if (currentId != lastId) {
                // new record

                if (currentEntry != null) {
                    glodmed.addEntry(currentEntry);
                }

                currentEntry = new GlodmedEntry(currentId, Glossary.valueOf(record.get(glossary)));

                lastId = currentId;
            }


            String mainLabel = record.get(mainterm).trim();


            // mainterm empty: bad record (should not happen)
            // mainterm set, subterm empty: regular concept
            // mainterm set, subterm set: subterm is concept name, mainterm is label of related concept

            if (!mainLabel.isEmpty()) {

                Language language = Language.valueOf(record.get(lang).trim().toUpperCase());

                GlodmedLanguageSpecificEntryPart languageSpecificEntryPart = new GlodmedLanguageSpecificEntryPart(currentEntry.getGlossary(), language);
                currentEntry.addLanguageSpecificPart(languageSpecificEntryPart);

                mainLabel = stripMarkup(mainLabel);

                String subLabel = record.get(subterm).trim();

                if (!subLabel.isEmpty()) {
                    subLabel = stripMarkup(subLabel);

                    String properSubLabel = properSubtermLabel(mainLabel, subLabel);
                    subLabel = mainLabel;
                    mainLabel = properSubLabel;

                    languageSpecificEntryPart.setSuperTerm(subLabel);
                }

                languageSpecificEntryPart.setLabel(mainLabel);
                languageSpecificEntryPart.setDefinition(record.get(definition).trim());
                languageSpecificEntryPart.setSee(record.get(see).trim());
                languageSpecificEntryPart.setSeeAlso(stripMarkup(record.get(see_also).trim()));
                languageSpecificEntryPart.setCompare(record.get(compare).trim());
                languageSpecificEntryPart.setReferencedLiterature(record.get(reference_literature).trim());
            }

        });

        // add the last one
        glodmed.addEntry(currentEntry);

        // second pass
        // construct output

        Constructor<GlodmedOwlOutputGenerator> constructor = mode.generator.getConstructor(Glodmed.class);
        GlodmedOutputGenerator output = constructor.newInstance(glodmed);

        glodmed.forEach(entry -> {

            output.addTerm("" + entry.getId(), entry.getGlossary());

            BooleanFlag superTermHandled = new BooleanFlag();

            entry.languageSpecificParts().forEach(part -> {

                Language language = part.getLanguage();

                // Super term references are stored redundantly for each language in glodmed. We only need to take care
                // of them once per entry.
                if (!superTermHandled.value) {
                    String superLabel = part.getSuperTerm();
                    if (superLabel != null) {
                        List<GlodmedEntry> superEntries = glodmed.getEntriesByLabel(entry.getGlossary(), part.getSuperTerm(), language);
                        superEntries.forEach(superEntry -> {
                            output.addSuperTermRelation("" + superEntry.getId());
                        });
                    }
                    superTermHandled.value = true;
                }

                Optional.of(part.getLabel()).filter(s -> !s.isEmpty()).ifPresent(label -> {
                    output.addLabel(label, language);
                });

                Optional.of(part.getDefinition()).filter(s -> !s.isEmpty()).ifPresent(definition -> {
                    output.addDefinition(definition, language);
                });

                Optional.of(part.getSee()).filter(s -> !s.isEmpty()).ifPresent(see -> {
                    Arrays.stream(see.split(";")).forEach(seeTerm -> {
                        glodmed.getEntriesByLabel(entry.getGlossary(), seeTerm.trim(), language).forEach(referencedEntry -> {
                            output.addSeeRelation("" + referencedEntry.getId());
                        });
                    });
                });

                Optional.of(part.getSeeAlso()).filter(s -> !s.isEmpty()).ifPresent(seeAlso -> {
                    Arrays.stream(seeAlso.split(";")).forEach(seeAlsoTerm -> {
                        glodmed.getEntriesByLabel(entry.getGlossary(), seeAlsoTerm.trim(), language).forEach(referencedEntry -> {
                            output.addSeeAlsoRelation("" + referencedEntry.getId());
                        });
                    });
                });

                Optional.of(part.getCompare()).filter(s -> !s.isEmpty()).ifPresent(compare -> {
                    Arrays.stream(compare.split(";")).forEach(compareTerm -> {
                        glodmed.getEntriesByLabel(entry.getGlossary(), compareTerm.trim(), language).forEach(referencedEntry -> {
                            output.addCompareRelation("" + referencedEntry.getId());
                        });
                    });
                });



            });

        });

        output.saveOutput(destination);
    }

    /*
     * pre-condition: mainterm.length > 0
     */
    private String properSubtermLabel(String mainterm, String subterm) {
        char firstChar = mainterm.toUpperCase().charAt(0);
        Pattern pattern = Pattern.compile("(^|[\\W])" + firstChar + "\\.");

        Matcher matcher = pattern.matcher(subterm);

        return matcher.replaceAll("$1" + mainterm);
    }

    private String stripMarkup(String orig) {
        return markupPattern.matcher(orig).replaceAll("");
    }

    private GlodmedEntry getPropertGlodmedEntry(String mainterm, String subterm) {
        return null;
    }

    // boolean flag for use inside lambdas
    private class BooleanFlag {
        public boolean value = false;
    }

    public static void main(String[] args) {
        if (args.length <2 || args.length > 3) {
            usageAndExit();
        }
        Mode mode = Mode.valueOf(args[0].replace('-', '_'));
        if (mode == null) {
            System.out.println("format must be one of owl, skos-rdf, or skos-owl\n");
            usageAndExit();
        }

        File sourceFile = new File(args[1]);
        if (!sourceFile.exists()) {
            System.out.format("File %s does not exist.\n\n", args[1]);
            usageAndExit();
        }
        if (sourceFile.isDirectory()) {
            System.out.format("%s is a directory but should be a file.\n\n", args[1]);
            usageAndExit();
        }

        File destination;

        if (args[2] != null) {
            destination = new File(args[2]);
        } else {
            File parentDir = sourceFile.getParentFile();
            String fileName = sourceFile.getName();
            switch (mode) {
                case owl:
                    destination = new File(parentDir, fileName + ".owl");
                    break;
                case skos_owl:
                    destination = new File(parentDir, fileName + ".skos.owl");
                    break;
                case skos_rdf:
                default:
                    destination = new File(parentDir, fileName + ".skos.rdf");
                    break;
            }
        }

        GlodmedConverter converter = new GlodmedConverter();
        try {
            converter.transform(mode, new FileReader(sourceFile), destination);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void usageAndExit() {
        System.out.format("Usage: java %s <format> <source-file> [<destination>]\n", GlodmedConverter.class.getName());
        System.out.println("<format>: owl, skosRDF, or skosOWL");
        System.out.println("<source-file>: path to the glodmed csv file in the local file system");
        System.out.println("<destination-url>: A (usually file) URL specifying the destination of the output");
        System.exit(-1);
    }
}
