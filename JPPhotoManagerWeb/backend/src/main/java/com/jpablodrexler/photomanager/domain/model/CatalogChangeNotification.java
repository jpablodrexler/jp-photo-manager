package com.jpablodrexler.photomanager.domain.model;

import com.jpablodrexler.photomanager.domain.model.Asset;
import com.jpablodrexler.photomanager.domain.enums.ReasonEnum;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CatalogChangeNotification {

    private ReasonEnum reason;
    private Asset asset;
    private String folderPath;
    private int percentCompleted;
    private String message;

    public CatalogChangeNotification(ReasonEnum reason, Asset asset, int percentCompleted) {
        this.reason = reason;
        this.asset = asset;
        this.percentCompleted = percentCompleted;
    }

    public CatalogChangeNotification(ReasonEnum reason, String folderPath, int percentCompleted) {
        this.reason = reason;
        this.folderPath = folderPath;
        this.percentCompleted = percentCompleted;
    }
}
