package com.focushive.events;

public class HiveCreatedEvent extends BaseEvent {
    private final String hiveId;
    private final String hiveName;
    private final String ownerId;
    private final boolean isPublic;
    
    public HiveCreatedEvent(String hiveId, String hiveName, String ownerId, boolean isPublic) {
        super(hiveId);
        this.hiveId = hiveId;
        this.hiveName = hiveName;
        this.ownerId = ownerId;
        this.isPublic = isPublic;
    }
    
    public String getHiveId() {
        return hiveId;
    }
    
    public String getHiveName() {
        return hiveName;
    }
    
    public String getOwnerId() {
        return ownerId;
    }
    
    public boolean isPublic() {
        return isPublic;
    }
}