package org.hibernate.bugs;

import jakarta.persistence.*;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@SecondaryTable(name = "test")
public class SecondaryTableEntitySub extends SecondaryTableEntityBase {

    private Long b;

    private Long c;

    @Column
    public Long getB() {
        return b;
    }

    public void setB(Long b) {
        this.b = b;
    }

    @Column(table = "test")
    public Long getC() {
        return c;
    }

    public void setC(Long c) {
        this.c = c;
    }
}