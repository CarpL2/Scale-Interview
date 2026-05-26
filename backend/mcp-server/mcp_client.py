import asyncio
import sys
import json
from mcp import ClientSession, StdioServerParameters
from mcp.client.stdio import stdio_client

async def main(company: str, position: str):
    # 配置要连接的 MCP Server (这里使用 stdio 方式运行刚才的 interview_mcp.py)
    server_params = StdioServerParameters(
        command="python",
        args=["interview_mcp.py"]
    )

    # 建立 MCP 连接
    async with stdio_client(server_params) as (read_stream, write_stream):
        async with ClientSession(read_stream, write_stream) as session:
            # 必须先初始化 MCP 握手
            await session.initialize()

            # 发起 Tool Call (调用面经搜索能力)
            result = await session.call_tool(
                "search_interview_questions",
                arguments={"company": company, "position": position}
            )

            # 输出给 Java 层
            if result.content:
                print(result.content[0].text)
            else:
                print("No content returned.")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python mcp_client.py <company> <position>")
        sys.exit(1)
        
    company = sys.argv[1]
    position = sys.argv[2]
    
    # 屏蔽 Windows 上的 ProactorEventLoop 报错
    if sys.platform == 'win32':
        asyncio.set_event_loop_policy(asyncio.WindowsSelectorEventLoopPolicy())
        
    asyncio.run(main(company, position))
