using System.ComponentModel.DataAnnotations;

namespace JWT4WebApi.Model
{
    public class Student
    {
        /// <summary>
        /// 学号
        /// </summary>
        [Required(ErrorMessage ="学号不能为空!")]
        public int? StuID { get; set; }

        /// <summary>
        /// 学生姓名
        /// </summary>
        [StringLength(20,ErrorMessage ="姓名最长不能超过20")]
        public string? StuName { get; set; }

        /// <summary>
        /// 电话号码
        /// </summary>
        [RegularExpression(@"^1([358][0-9]|4[579]|66|7[0135678]|9[89])[0-9]{8}$",ErrorMessage ="电话号码格式不正确!")]
        public string? PhoneNum { get; set; }
        /// <summary>
        /// 电子邮箱
        /// </summary>
        [EmailAddress(ErrorMessage ="邮箱格式不正确!")]
        public string? Email { get; set; }
    }
}
