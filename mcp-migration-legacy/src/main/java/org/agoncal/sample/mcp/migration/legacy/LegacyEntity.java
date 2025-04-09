package org.agoncal.sample.mcp.migration.legacy;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.sql.Date;
import java.sql.Time;

@Entity
public class LegacyEntity {

    @Id
    private Long id;

    @Temporal(TemporalType.DATE)
    private Date legacyDate;

    @Temporal(TemporalType.TIME)
    private Time legacyTime;

    public LegacyEntity(Long id, Date legacyDate) {
        this.id = id;
        this.legacyDate = legacyDate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Date getLegacyDate() {
        return legacyDate;
    }

    public void setLegacyDate(Date legacyDate) {
        this.legacyDate = legacyDate;
    }
}
