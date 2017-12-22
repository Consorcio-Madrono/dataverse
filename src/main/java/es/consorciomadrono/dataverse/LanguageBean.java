package es.consorciomadrono.dataverse;

import java.io.Serializable;
import java.util.Locale;
import javax.annotation.PostConstruct;

import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.context.FacesContext;

@ManagedBean
@SessionScoped
@Named("language")
public class LanguageBean implements Serializable{
    @ManagedProperty(value = "#{locale}")
    
    private Locale locale;

    
    public Locale getLocale() {
        return locale;
    }

    public String getLanguage() {
        return locale.getLanguage();
    }

    @PostConstruct
    public void init() {
        locale = FacesContext.getCurrentInstance().getExternalContext().getRequestLocale();
    }

    public synchronized String changeLanguage(String lang, String country) {
        Locale newLocale= new Locale(lang, country);
        FacesContext.getCurrentInstance().getViewRoot().setLocale(newLocale);
        locale= newLocale;
        Locale.setDefault(newLocale);
        return null;
    }
}
