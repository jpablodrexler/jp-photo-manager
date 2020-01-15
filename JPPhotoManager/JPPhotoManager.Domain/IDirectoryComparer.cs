using System.Collections.Generic;

namespace JPPhotoManager.Domain
{
    public interface IDirectoryComparer
    {
        string[] GetDeletedFileNames(string[] fileNames, List<Asset> cataloguedAssets);
        string[] GetNewFileNames(string[] fileNames, List<Asset> cataloguedAssets);
        string[] GetNewFileNames(string[] sourceFileNames, string[] destinationFileNames);
    }
}
