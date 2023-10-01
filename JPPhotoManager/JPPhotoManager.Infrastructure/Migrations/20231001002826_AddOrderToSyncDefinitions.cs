using Microsoft.EntityFrameworkCore.Migrations;

#nullable disable

namespace JPPhotoManager.Infrastructure.Migrations
{
    public partial class AddOrderToSyncDefinitions : Migration
    {
        protected override void Up(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.AddColumn<int>(
                name: "Order",
                table: "SyncAssetsDirectoriesDefinitions",
                type: "INTEGER",
                nullable: false,
                defaultValue: 0);
        }

        protected override void Down(MigrationBuilder migrationBuilder)
        {
            migrationBuilder.DropColumn(
                name: "Order",
                table: "SyncAssetsDirectoriesDefinitions");
        }
    }
}
