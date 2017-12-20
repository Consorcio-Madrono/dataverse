package es.consorciomadrono.dataverse;

import java.io.Serializable;
import java.util.Locale;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;

@SessionScoped
@Named("language")
public class LanguageBean implements Serializable{
    public synchronized String changeLanguage(String lang, String country) {
        FacesContext.getCurrentInstance().getViewRoot().setLocale(new Locale(lang, country));
        return "changed";
    }
}
