package de.simpleanno.glodmed;

public enum Language {EN("en"), DE("de"), FR("fr"), IT("it"), PO("pt"), SP("es");
    public String isoCode;
    Language(String isoCode) {this.isoCode = isoCode;}
}
