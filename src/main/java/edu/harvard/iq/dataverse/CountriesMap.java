package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author juancorr
 * 
 * CONSORCIO MADROÑO. New class to read the country codes for the statistics 
 * visualization
 * 
 */
@Entity
@Table (name = "countriesmap" , indexes = {@Index(columnList="id")})
@NamedQueries({
    @NamedQuery(name = "CountriesMap.findAll", query= "SELECT f FROM CountriesMap f")
})
public class CountriesMap implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", columnDefinition = "TEXT", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    
    @Column(name = "name", columnDefinition = "TEXT", nullable = false)
    private String name;

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        hash += (id != null ? id.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        // TODO: Warning - this method won't work in the case the code fields are not set
        if (!(object instanceof CountriesMap)) {
            return false;
        }
        CountriesMap other = (CountriesMap) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.CountriesMap[ id=" + id + " ]";
    }
}
