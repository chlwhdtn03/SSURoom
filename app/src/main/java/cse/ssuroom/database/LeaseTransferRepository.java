package cse.ssuroom.database;


public class LeaseTransferRepository extends PropertyRepository<LeaseTransfer> {


    public LeaseTransferRepository() {
        super("lease_transfers", LeaseTransfer.class, "LeaseTransferRepo");
    }


}
