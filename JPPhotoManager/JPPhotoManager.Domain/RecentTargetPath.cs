using System.ComponentModel.DataAnnotations;

namespace JPPhotoManager.Domain
{
    public class RecentTargetPath
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public string Path { get; set; }
    }
}
