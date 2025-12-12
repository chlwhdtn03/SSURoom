package cse.ssuroom.database;


public class ShortTermRepository extends PropertyRepository<ShortTerm> {


    public ShortTermRepository() {
        super("short_terms", ShortTerm.class, "ShortTermRepo");
    }

}
