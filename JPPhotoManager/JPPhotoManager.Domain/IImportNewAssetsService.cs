using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface IImportNewAssetsService
    {
        List<ImportNewAssetsResult> Import();
    }
}