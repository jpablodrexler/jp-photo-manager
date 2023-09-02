using System;
using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace JPPhotoManager.Infrastructure.Migrations
{
    public partial class AddSyncAssetsDirectoriesDefinition : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "SyncAssetsDirectoriesDefinitions",
                columns: table => new
                {
                    Id = table.Column<Guid>(type: "TEXT", nullable: false),
                    SourceDirectory = table.Column<string>(type: "TEXT", nullable: false),
                    DestinationDirectory = table.Column<string>(type: "TEXT", nullable: false),
                    IncludeSubFolders = table.Column<bool>(type: "INTEGER", nullable: false),
                    DeleteAssetsNotInSource = table.Column<bool>(type: "INTEGER", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_SyncAssetsDirectoriesDefinitions", x => x.Id);
                });
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "SyncAssetsDirectoriesDefinitions");
        }
    }
}
