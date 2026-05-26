from mcp.server.fastmcp import FastMCP
import sys

# 初始化 MCP Server
mcp = FastMCP("InterviewQuestions")

# 定义一个获取面经的 Tool
@mcp.tool()
def search_interview_questions(company: str, position: str) -> str:
    """
    Search for real interview questions from specific companies.
    Args:
        company: The name of the company (e.g., '字节跳动', '阿里', '腾讯')
        position: The job position (e.g., 'Java后端', '前端', '算法')
    """
    print(f"[MCP LOG] 接收到搜索请求: {company} - {position}", file=sys.stderr)
    
    # 这里为了演示，我们使用硬编码的真实面经。
    # 实际上这里可以写 requests.get() 去抓取牛客网或者请求真实的数据库。
    if "字节" in company or "bytedance" in company.lower():
        return f"【{company} {position} 面经题库返回】\n" \
               "1. 算法题：反转链表（要求O(1)空间复杂度），接雨水。\n" \
               "2. 基础题：Redis的ZSet底层数据结构是什么？跳表是怎么插入的？\n" \
               "3. 场景题：头条文章点赞数如果瞬间暴增（10万QPS），你会怎么设计Redis和DB的缓存一致性？\n" \
               "4. 操作系统：协程和线程的区别？Go/Java中的协程是怎么调度的？"
    
    elif "阿里" in company or "alibaba" in company.lower():
        return f"【{company} {position} 面经题库返回】\n" \
               "1. 源码题：Spring Boot的自动装配原理？AOP的动态代理CGLIB和JDK代理的区别？\n" \
               "2. 中间件：RocketMQ事务消息的二阶段提交是怎么做的？如何保证消息不丢失？\n" \
               "3. 并发编程：ConcurrentHashMap的扩容机制？CountDownLatch和CyclicBarrier的区别？\n" \
               "4. 数据库：MySQL索引下推（ICP）是什么？为什么主键要推荐用自增ID？"
               
    elif "腾讯" in company or "tencent" in company.lower():
        return f"【{company} {position} 面经题库返回】\n" \
               "1. 网络协议：TCP的三次握手和四次挥手？TIME_WAIT状态有什么用？\n" \
               "2. 基础：C++的虚函数表是怎么实现的？/ Java的垃圾回收机制G1和CMS的区别？\n" \
               "3. 场景题：微信红包系统怎么设计？如何防止超发？\n" \
               "4. 海量数据：100亿个URL中找出重复的，只有2G内存怎么做？（布隆过滤器或分治哈希）"
               
    else:
        return f"【通用 {position} 面试题库】\n" \
               "1. 讲讲你最熟悉的一个项目，遇到的最大难点是什么，怎么解决的？\n" \
               "2. 数据库事务隔离级别有哪些？MySQL默认是哪种？\n" \
               "3. Redis常用的数据结构及使用场景？\n" \
               "4. JVM内存模型是怎样的？OOM通常怎么排查？"

if __name__ == "__main__":
    # 使用 stdio 方式运行（这是 MCP 跨语言通讯最标准的方式）
    mcp.run(transport='stdio')
