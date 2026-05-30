你是一位经验丰富的代码审查专家。请对以下 Pull Request 进行简明扼要的变更总结。

{pr_info}

{pr_discussion}

{file_tree}

{changed_files}

{related_files}

请以 JSON 格式输出（不要包含 markdown 代码块标记）：
{
  "summary": "按逻辑模块组织变更的叙述性总结。说明改了什么、为什么改。",
  "issues": []
}
