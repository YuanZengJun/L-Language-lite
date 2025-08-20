package ldk.l.litec.util.error

/**
 * 自动生成错误代码的枚举类
 * 错误代码格式：E + 4位数字（从0001开始）
 * 新增枚举值时无需手动指定code，会自动按顺序生成
 */
enum class ErrorType(
    val message: String,       // 错误简短描述
    val template: String       // 详细信息模板
) {
    // ===================== 词法错误（Lexical Errors）=====================
    ILLEGAL_CHARACTER(
        "输入非法",
        "非法字符 '%s'"
    ),
    UNTERMINATED_STRING(
        "未结束的字符串",
        "字符串缺少闭合引号，从第 %d 行开始"
    ),
    INVALID_ESCAPE_SEQUENCE(
        "无效的转义序列",
        "不支持的转义字符 '\\%s'"
    ),
    INVALID_NUMBER_FORMAT(
        "数字格式错误",
        "无效的数字格式: '%s'"
    ),
    INVALID_IDENTIFIER(
        "标识符格式错误",
        "标识符 '%s' 包含无效字符（仅支持字母、数字、下划线）"
    ),
    INVALID_EMOJI(
        "不支持Emoji",
        "字符 '%s' 为非法字符,不支持Emoji"
    ),
    INVALID_OPERATOR(
        "不支持的操作符",
        "字符 '%s' 为非法操作符"
    ),
    CONTROL_CHARACTER_NOT_ALLOWED(
        "控制字符不允许",
        "不支持的控制字符 '\\u%04X'（如退格、制表符等不可见字符）"
    ),
    INVALID_EMOJI_CHAR(
        "无效的表情符号",
        "不支持的表情符号或代理对序列（当前仅支持标准手势/表情类emoji）"
    ),
    CHINESE_QUOTE_NOT_ALLOWED(
        "中文引号不允许",
        "应使用英文引号（\"），而非中文引号 '%s'（如“”‘’）"
    ),
    CHINESE_PUNCTUATION_NOT_ALLOWED(
        "中文标点不允许",
        "应使用英文标点，而非中文标点 '%s'（如，。；：）"
    ),
    UNKNOWN_OPERATOR(
        "未知运算符",
        "不支持的运算符 '%s'（仅支持 +、-、*、/、=、==、!=、>、<、>=、<=）"
    ),
    UNTERMINATED_COMMENT(
        "未结束的注释",
        "多行注释缺少闭合标记 '*/'，从第 %d 行开始"
    ),

    // ===================== 语法错误（Syntactic Errors）=====================
    UNEXPECTED_TOKEN(
        "意外的标记",
        "预期 '%s' 但找到 '%s'"
    ),
    MISSING_SEMICOLON(
        "缺少分号",
        "语句结束处缺少分号（第 %d 行）"
    ),
    UNTERMINATED_BLOCK(
        "未结束的代码块",
        "代码块缺少闭合 '}'，从第 %d 行 '{' 开始"
    ),
    MISSING_PARENTHESIS(
        "缺少括号",
        "缺少闭合括号 '%s'（对应第 %d 行的 '%s'）"
    ),
    INVALID_STATEMENT_STRUCTURE(
        "语句结构无效",
        "此处不能使用 '%s' 作为独立语句（第 %d 行）"
    ),
    MISSING_CONDITION_IN_IF(
        "if语句缺少条件",
        "if 关键字后缺少条件表达式（第 %d 行）"
    ),
    MISSING_CONDITION_IN_WHILE(
        "while语句缺少条件",
        "while 关键字后缺少循环条件表达式（第 %d 行）"
    ),
    EMPTY_FUNCTION_PARAM_LIST(
        "函数参数列表为空",
        "函数参数列表不能为空（若无需参数，需写 '()'，第 %d 行）"
    ),
    MISSING_FUNCTION_BODY(
        "函数缺少函数体",
        "函数 '%s' 声明后缺少函数体（需用 '{}' 包裹，第 %d 行）"
    ),
    INVALID_RETURN_POSITION(
        "return语句位置错误",
        "return 语句不能出现在函数体外（第 %d 行）"
    ),
    MISSING_COMMA_IN_PARAMS(
        "参数列表缺少逗号",
        "函数参数 '%s' 后缺少逗号分隔（第 %d 行）"
    ),

    // ===================== 语义错误（Semantic Errors）=====================
    UNDEFINED_SYMBOL(
        "使用未声明的符号",
        "符号 '%s' 尚未声明（可能是变量/函数名拼写错误）"
    ),
    TYPE_MISMATCH(
        "类型不匹配",
        "预期类型 '%s' 但找到 '%s'（第 %d 行）"
    ),
    SYMBOL_ALREADY_DEFINED(
        "符号已重复定义",
        "符号 '%s' 已在当前作用域声明（第 %d 行）"
    ),
    FUNCTION_NOT_FOUND(
        "函数未找到",
        "未找到函数 '%s' 的定义（检查函数名拼写或参数列表）"
    ),
    PARAM_COUNT_MISMATCH(
        "函数参数数量不匹配",
        "调用函数 '%s' 时参数数量不匹配（预期 %d 个，实际 %d 个）"
    ),
    PARAM_TYPE_MISMATCH(
        "函数参数类型不匹配",
        "函数 '%s' 的第 %d 个参数：预期 '%s' 但找到 '%s'"
    ),
    CANNOT_CALL_NON_FUNCTION(
        "非函数类型无法调用",
        "'%s' 是 %s 类型，不是函数，无法执行调用（第 %d 行）"
    ),
    UNINITIALIZED_VARIABLE(
        "使用未初始化的变量",
        "变量 '%s' 在赋值前被使用（第 %d 行）"
    ),
    READ_ONLY_VARIABLE_ASSIGN(
        "只读变量无法赋值",
        "变量 '%s' 是只读类型，不能重新赋值（第 %d 行）"
    ),
    TYPE_CANNOT_OPERATE(
        "类型不支持该操作",
        "无法对 '%s' 类型和 '%s' 类型执行 '%s' 操作（第 %d 行）"
    ),
    FUNCTION_RETURN_TYPE_MISMATCH(
        "函数返回值类型不匹配",
        "函数 '%s' 预期返回 '%s' 类型，但实际返回 '%s' 类型"
    ),
    MISSING_RETURN_IN_FUNCTION(
        "函数缺少return语句",
        "有返回值的函数 '%s' 必须包含return语句（第 %d 行）"
    ),

    // ===================== 运行时错误（Runtime Errors）=====================
    DIVISION_BY_ZERO(
        "除以零错误",
        "算术运算中出现除以零的错误（第 %d 行）"
    ),
    INDEX_OUT_OF_BOUNDS(
        "索引越界",
        "集合索引 %d 超出范围（有效范围：0 至 %d，第 %d 行）"
    ),
    NULL_POINTER_ACCESS(
        "空指针访问",
        "尝试访问空对象的属性或方法（第 %d 行）"
    ),
    STRING_INDEX_OUT_OF_BOUNDS(
        "字符串索引越界",
        "字符串索引 %d 超出范围（字符串长度：%d，第 %d 行）"
    ),
    VALUE_TOO_LARGE(
        "数值超出范围",
        "数值 '%s' 超出 %s 类型的最大范围（第 %d 行）"
    );

    // 自动生成错误代码：E + 4位数字（从0001开始，ordinal从0递增）
    val code: String = "E%04d".format(ordinal + 1)
}