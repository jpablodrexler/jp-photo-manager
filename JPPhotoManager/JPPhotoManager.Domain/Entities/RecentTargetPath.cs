using System.ComponentModel.DataAnnotations;

namespace JPPhotoManager.Domain.Entities
{
    public class RecentTargetPath
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public string Path { get; set; }
    }
}
