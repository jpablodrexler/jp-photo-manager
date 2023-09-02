using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace JPPhotoManager.Infrastructure.Migrations
{
    public partial class InitialMigration : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "Folders",
                columns: table => new
                {
                    FolderId = table.Column<string>(type: "TEXT", nullable: false),
                    Path = table.Column<string>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Folders", x => x.FolderId);
                });

            migrationBuilder.CreateTable(
                name: "Assets",
                columns: table => new
                {
                    AssetId = table.Column<string>(type: "TEXT", nullable: false),
                    FolderId = table.Column<string>(type: "TEXT", nullable: false),
                    FileName = table.Column<string>(type: "TEXT", nullable: false),
                    FileSize = table.Column<long>(type: "INTEGER", nullable: false),
                    PixelWidth = table.Column<int>(type: "INTEGER", nullable: false),
                    PixelHeight = table.Column<int>(type: "INTEGER", nullable: false),
                    ThumbnailPixelWidth = table.Column<int>(type: "INTEGER", nullable: false),
                    ThumbnailPixelHeight = table.Column<int>(type: "INTEGER", nullable: false),
                    ImageRotation = table.Column<int>(type: "INTEGER", nullable: false),
                    ThumbnailCreationDateTime = table.Column<DateTime>(type: "TEXT", nullable: false),
                    Hash = table.Column<string>(type: "TEXT", nullable: false),
                    FileCreationDateTime = table.Column<DateTime>(type: "TEXT", nullable: false),
                    FileModificationDateTime = table.Column<DateTime>(type: "TEXT", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_Assets", x => x.AssetId);
                    table.ForeignKey(
                        name: "FK_Assets_Folders_FolderId",
                        column: x => x.FolderId,
                        principalTable: "Folders",
                        principalColumn: "FolderId",
                        onDelete: ReferentialAction.Cascade);
                });

            migrationBuilder.CreateIndex(
                name: "IX_Assets_FolderId",
                table: "Assets",
                column: "FolderId");
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "Assets");

            migrationBuilder.DropTable(
                name: "Folders");
        }
    }
}
