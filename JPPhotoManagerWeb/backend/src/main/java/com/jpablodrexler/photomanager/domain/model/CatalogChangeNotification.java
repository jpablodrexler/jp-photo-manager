package com.jpablodrexler.photomanager.domain.model;

import com.jpablodrexler.photomanager.domain.enums.Reason;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CatalogChangeNotification {

    private Reason reason;
    private Asset asset;
    private String folderPath;
    private int percentCompleted;
    private String message;

    public CatalogChangeNotification(Reason reason, Asset asset, int percentCompleted) {
        this.reason = reason;
        this.asset = asset;
        this.percentCompleted = percentCompleted;
    }

    public CatalogChangeNotification(Reason reason, String folderPath, int percentCompleted) {
        this.reason = reason;
        this.folderPath = folderPath;
        this.percentCompleted = percentCompleted;
    }
}
