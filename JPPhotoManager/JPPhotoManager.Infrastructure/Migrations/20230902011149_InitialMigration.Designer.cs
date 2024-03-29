﻿// <auto-generated />
using System;
using JPPhotoManager.Infrastructure;
using Microsoft.EntityFrameworkCore;
using Microsoft.EntityFrameworkCore.Infrastructure;
using Microsoft.EntityFrameworkCore.Migrations;
using Microsoft.EntityFrameworkCore.Storage.ValueConversion;

#nullable disable

namespace JPPhotoManager.Infrastructure.Migrations
{
    [DbContext(typeof(AppDbContext))]
    [Migration("20230902011149_InitialMigration")]
    partial class InitialMigration
    {
        protected override void BuildTargetModel(ModelBuilder modelBuilder)
        {
#pragma warning disable 612, 618
            modelBuilder.HasAnnotation("ProductVersion", "6.0.21");

            modelBuilder.Entity("JPPhotoManager.Domain.Asset", b =>
                {
                    b.Property<string>("AssetId")
                        .HasColumnType("TEXT");

                    b.Property<DateTime>("FileCreationDateTime")
                        .HasColumnType("TEXT");

                    b.Property<DateTime>("FileModificationDateTime")
                        .HasColumnType("TEXT");

                    b.Property<string>("FileName")
                        .IsRequired()
                        .HasColumnType("TEXT");

                    b.Property<long>("FileSize")
                        .HasColumnType("INTEGER");

                    b.Property<string>("FolderId")
                        .IsRequired()
                        .HasColumnType("TEXT");

                    b.Property<string>("Hash")
                        .IsRequired()
                        .HasColumnType("TEXT");

                    b.Property<int>("ImageRotation")
                        .HasColumnType("INTEGER");

                    b.Property<int>("PixelHeight")
                        .HasColumnType("INTEGER");

                    b.Property<int>("PixelWidth")
                        .HasColumnType("INTEGER");

                    b.Property<DateTime>("ThumbnailCreationDateTime")
                        .HasColumnType("TEXT");

                    b.Property<int>("ThumbnailPixelHeight")
                        .HasColumnType("INTEGER");

                    b.Property<int>("ThumbnailPixelWidth")
                        .HasColumnType("INTEGER");

                    b.HasKey("AssetId");

                    b.HasIndex("FolderId");

                    b.ToTable("Assets");
                });

            modelBuilder.Entity("JPPhotoManager.Domain.Folder", b =>
                {
                    b.Property<string>("FolderId")
                        .HasColumnType("TEXT");

                    b.Property<string>("Path")
                        .IsRequired()
                        .HasColumnType("TEXT");

                    b.HasKey("FolderId");

                    b.ToTable("Folders");
                });

            modelBuilder.Entity("JPPhotoManager.Domain.Asset", b =>
                {
                    b.HasOne("JPPhotoManager.Domain.Folder", "Folder")
                        .WithMany()
                        .HasForeignKey("FolderId")
                        .OnDelete(DeleteBehavior.Cascade)
                        .IsRequired();

                    b.Navigation("Folder");
                });
#pragma warning restore 612, 618
        }
    }
}
