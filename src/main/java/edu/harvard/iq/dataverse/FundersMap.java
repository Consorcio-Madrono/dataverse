package edu.harvard.iq.dataverse;

import java.io.Serializable;
import javax.persistence.*;

/**
 *
 * @author juancorr
 * 
 * CONSORCIO MADROÑO. New class to read the funder dois from the fundersmap table to 
 * have a better OpenAIRE compability
 * 
 */
@Entity
@Table (name = "fundersmap" , indexes = {@Index(columnList="id")})
@NamedQueries({
    @NamedQuery(name = "FundersMap.findAll", query= "SELECT f FROM FundersMap f")
})
public class FundersMap implements Serializable {
    private static final long serialVersionUID = 1L;
    @Id
    @Column(name = "id", columnDefinition = "TEXT", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private String id;
    
    @Column(name = "doi", columnDefinition = "TEXT", nullable = false)
    private String doi;

    public String getDoi() {
        return this.doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
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
        // TODO: Warning - this method won't work in the case the id fields are not set
        if (!(object instanceof FundersMap)) {
            return false;
        }
        FundersMap other = (FundersMap) object;
        if ((this.id == null && other.id != null) || (this.id != null && !this.id.equals(other.id))) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return "edu.harvard.iq.dvn.core.vdc.FundersMap[ id=" + id + " ]";
    }
}
