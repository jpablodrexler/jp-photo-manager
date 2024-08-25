using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace JPPhotoManager.Infrastructure.Migrations
{
    /// <inheritdoc />
    public partial class AddConvertAssetsDirectoriesDefinition : Migration
    {
        /// <inheritdoc />
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.CreateTable(
                name: "ConvertAssetsDirectoriesDefinitions",
                columns: table => new
                {
                    Id = table.Column<int>(type: "INTEGER", nullable: false)
                        .Annotation("Sqlite:Autoincrement", true),
                    SourceDirectory = table.Column<string>(type: "TEXT", nullable: false),
                    DestinationDirectory = table.Column<string>(type: "TEXT", nullable: false),
                    IncludeSubFolders = table.Column<bool>(type: "INTEGER", nullable: false),
                    DeleteAssetsNotInSource = table.Column<bool>(type: "INTEGER", nullable: false),
                    Order = table.Column<int>(type: "INTEGER", nullable: false)
                },
                constraints: table =>
                {
                    table.PrimaryKey("PK_ConvertAssetsDirectoriesDefinitions", x => x.Id);
                });
        }

        /// <inheritdoc />
        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropTable(
                name: "ConvertAssetsDirectoriesDefinitions");
        }
    }
}
