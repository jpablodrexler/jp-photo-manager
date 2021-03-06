﻿using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;

namespace JPPhotoManager.Domain
{
    public class MoveAssetsService : IMoveAssetsService
    {
        private const int RECENT_TARGET_PATHS_MAX_COUNT = 20;
        private readonly IAssetRepository assetRepository;
        private readonly IStorageService storageService;
        private readonly ICatalogAssetsService catalogAssetsService;

        public MoveAssetsService(
            IAssetRepository assetRepository,
            IStorageService storageService,
            ICatalogAssetsService catalogAssetsService)
        {
            this.assetRepository = assetRepository;
            this.storageService = storageService;
            this.catalogAssetsService = catalogAssetsService;
        }

        public bool MoveAsset(Asset asset, Folder destinationFolder, bool preserveOriginalFile)
        {
            #region Parameters validation

            if (asset == null)
            {
                throw new ArgumentNullException(nameof(asset), "asset cannot be null.");
            }

            if (asset.Folder == null)
            {
                throw new ArgumentNullException(nameof(asset), "asset.Folder cannot be null.");
            }

            if (destinationFolder == null)
            {
                throw new ArgumentNullException(nameof(destinationFolder), "destinationFolder cannot be null.");
            }

            #endregion

            bool result = false;
            string sourcePath = asset.FullPath;
            string destinationPath = Path.Combine(destinationFolder.Path, asset.FileName);
            bool isDestinationFolderInCatalog;

            if (!this.storageService.FileExists(sourcePath))
            {
                // This could happen if an image was moved or deleted outside the app.
                // TODO: Instead of just failing, should remove the file from the catalog.
                throw new ArgumentException(sourcePath);
            }

            var folder = this.assetRepository.GetFolderByPath(destinationFolder.Path);

            // If the folder is null, it means is not present in the catalog.
            // TODO: IF THE DESTINATION FOLDER IS NEW, THE FOLDER NAVIGATION CONTROL SHOULD DISPLAY IT WHEN THE USER GOES BACK TO THE MAIN WINDOW.
            isDestinationFolderInCatalog = folder != null;

            if (isDestinationFolderInCatalog)
            {
                destinationFolder = folder;
            }

            if (this.storageService.FileExists(sourcePath) && !this.storageService.FileExists(destinationPath))
            {
                result = this.storageService.CopyImage(sourcePath, destinationPath);

                if (result)
                {
                    if (!preserveOriginalFile)
                    {
                        this.DeleteAsset(asset, deleteFile: true);
                    }

                    if (!this.assetRepository.FolderExists(destinationFolder.Path))
                    {
                        destinationFolder = this.assetRepository.AddFolder(destinationFolder.Path);
                    }

                    this.catalogAssetsService.CreateAsset(destinationFolder.Path, asset.FileName);
                    AddTargetPathToRecent(destinationFolder);
                    this.assetRepository.SaveCatalog(destinationFolder);
                }
            }

            return result;
        }

        // TODO: Extend automated tests to evaluate recent target paths.
        private void AddTargetPathToRecent(Folder destinationFolder)
        {
            List<string> recentTargetPaths = this.assetRepository.GetRecentTargetPaths();

            if (recentTargetPaths.Contains(destinationFolder.Path))
                recentTargetPaths.Remove(destinationFolder.Path);

            recentTargetPaths.Insert(0, destinationFolder.Path);

            recentTargetPaths = recentTargetPaths.Take(RECENT_TARGET_PATHS_MAX_COUNT).ToList();
            this.assetRepository.SetRecentTargetPaths(recentTargetPaths);
        }

        public void DeleteAsset(Asset asset, bool deleteFile)
        {
            #region Parameters validation

            if (asset == null)
            {
                throw new ArgumentNullException(nameof(asset), "Asset cannot be null.");
            }

            if (asset.Folder == null)
            {
                throw new ArgumentNullException(nameof(asset), "Asset.Folder cannot be null.");
            }

            if (deleteFile && !this.storageService.FileExists(asset, asset.Folder))
            {
                throw new ArgumentException("File does not exist: " + asset.FullPath);
            }

            #endregion

            this.assetRepository.DeleteAsset(asset.Folder.Path, asset.FileName);

            if (deleteFile)
            {
                this.storageService.DeleteFile(asset.Folder.Path, asset.FileName);
            }

            this.assetRepository.SaveCatalog(asset.Folder);
        }
    }
}
