import re
import requests

# 测试 GitHub API
def test_github_api():
    print("=== 测试 GitHub API ===")
    url = "https://api.github.com/repos/LuckyLearning/legado/releases/latest"
    try:
        headers = {
            "Accept": "application/vnd.github.v3+json"
        }
        response = requests.get(url, headers=headers)
        print(f"API 响应状态码: {response.status_code}")
        if response.status_code == 200:
            data = response.json()
            print(f"版本号: {data.get('tag_name')}")
            print(f"发布时间: {data.get('created_at')}")
            print(f"更新日志: {data.get('body', '')[:100]}...")
            if 'assets' in data:
                for asset in data['assets']:
                    if asset['name'].endswith('.apk'):
                        print(f"APK 下载链接: {asset['browser_download_url']}")
        elif response.status_code == 403:
            print("API 速率限制，尝试 HTML 解析")
        else:
            print(f"API 请求失败: {response.status_code}")
    except Exception as e:
        print(f"API 请求异常: {e}")

# 测试 HTML 解析
def test_html_parse():
    print("\n=== 测试 HTML 解析 ===")
    
    # 尝试使用不同的用户代理
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    
    # 尝试直接访问一个具体的发布页面
    urls = [
        "https://github.com/LuckyLearning/legado/releases/latest",
        "https://github.com/LuckyLearning/legado/releases/tag/3.26.022622",
        "https://github.com/LuckyLearning/legado/releases"
    ]
    
    for url in urls:
        print(f"\n尝试访问: {url}")
        try:
            response = requests.get(url, headers=headers, allow_redirects=True)
            response.raise_for_status()
            html = response.text
            print(f"重定向后的 URL: {response.url}")
            print(f"页面长度: {len(html)} 字符")
            
            # 检查页面中是否有 APK 下载链接
            print("\n=== 检查 APK 链接 ===")
            
            # 尝试直接从页面中提取版本号
            version_match = re.search(r'tag/([^/]+)', response.url)
            if version_match:
                version = version_match.group(1)
                print(f"从 URL 提取的版本号: {version}")
                
                # 构建可能的 APK 下载链接
                possible_urls = [
                    f"https://github.com/LuckyLearning/legado/releases/download/{version}/legado_app_{version}.apk",
                    f"https://github.com/LuckyLearning/legado/releases/download/{version}/legado_{version}.apk",
                    f"https://github.com/LuckyLearning/legado/releases/download/{version}/app_{version}.apk"
                ]
                
                print("\n尝试直接访问可能的 APK 链接:")
                for apk_url in possible_urls:
                    try:
                        apk_response = requests.head(apk_url, headers=headers, allow_redirects=True)
                        if apk_response.status_code == 200:
                            print(f"✓ 找到有效的 APK 链接: {apk_url}")
                        else:
                            print(f"✗ 链接无效 ({apk_response.status_code}): {apk_url}")
                    except Exception as e:
                        print(f"✗ 访问失败: {apk_url}")
            
            # 尝试不同的正则表达式
            regex_patterns = [
                r'<a[^>]+href="([^"\\s]+\\.apk)"[^>]*>',
                r'href="([^"\\s]+\\.apk)"',
                r'https://[^"\\s]+\\.apk'
            ]
            
            found = False
            for i, pattern in enumerate(regex_patterns):
                regex = re.compile(pattern)
                matches = regex.finditer(html)
                for match in matches:
                    found = True
                    download_url = match.group(1) if len(match.groups()) > 0 else match.group(0)
                    if not download_url.startswith("http"):
                        download_url = "https://github.com" + download_url
                    print(f"找到 APK 链接: {download_url}")
            
            if not found:
                print("未找到 APK 链接")
                # 打印页面标题
                title_match = re.search(r'<title>([^<]+)</title>', html)
                if title_match:
                    print(f"页面标题: {title_match.group(1)}")
        except Exception as e:
            print(f"请求异常: {e}")

# 测试直接构建 APK 链接
def test_direct_apk_links():
    print("\n=== 测试直接构建 APK 链接 ===")
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    }
    
    # 尝试不同的版本号格式
    versions = [
        "3.26.022622",
        "3.25.062020",
        "3.24.051518",
        "3.23.041016"
    ]
    
    for version in versions:
        print(f"\n测试版本: {version}")
        # 构建可能的 APK 下载链接
        possible_urls = [
            f"https://github.com/LuckyLearning/legado/releases/download/{version}/legado_app_{version}.apk",
            f"https://github.com/LuckyLearning/legado/releases/download/{version}/legado_{version}.apk",
            f"https://github.com/LuckyLearning/legado/releases/download/{version}/app_{version}.apk",
            f"https://github.com/LuckyLearning/legado/releases/download/v{version}/legado_app_{version}.apk",
            f"https://github.com/LuckyLearning/legado/releases/download/v{version}/legado_{version}.apk",
            f"https://github.com/LuckyLearning/legado/releases/download/v{version}/app_{version}.apk"
        ]
        
        for apk_url in possible_urls:
            try:
                apk_response = requests.head(apk_url, headers=headers, allow_redirects=True)
                if apk_response.status_code == 200:
                    print(f"✓ 找到有效的 APK 链接: {apk_url}")
                    # 测试下载
                    print("  测试下载...")
                    apk_response = requests.get(apk_url, headers=headers, stream=True)
                    if apk_response.status_code == 200:
                        print(f"  ✓ 下载成功，文件大小: {len(apk_response.content) / 1024 / 1024:.2f} MB")
                    else:
                        print(f"  ✗ 下载失败 ({apk_response.status_code})")
                    break
                else:
                    print(f"✗ 链接无效 ({apk_response.status_code}): {apk_url}")
            except Exception as e:
                print(f"✗ 访问失败: {apk_url}")

# 模拟完整的版本检查流程
def test_full_flow():
    print("\n=== 测试完整版本检查流程 ===")
    # 首先尝试 API
    api_success = False
    try:
        url = "https://api.github.com/repos/LuckyLearning/legado/releases/latest"
        headers = {
            "Accept": "application/vnd.github.v3+json"
        }
        response = requests.get(url, headers=headers)
        if response.status_code == 200:
            data = response.json()
            print("✓ API 请求成功")
            print(f"版本号: {data.get('tag_name')}")
            api_success = True
        else:
            print(f"✗ API 请求失败: {response.status_code}")
    except Exception as e:
        print(f"✗ API 请求异常: {e}")
    
    # 如果 API 失败，尝试 HTML 解析
    if not api_success:
        print("尝试 HTML 解析...")
        try:
            url = "https://github.com/LuckyLearning/legado/releases/latest"
            response = requests.get(url, allow_redirects=True)
            response.raise_for_status()
            html = response.text
            
            # 提取下载链接
            download_regex = re.compile(r'<a href="(/LuckyLearning/legado/releases/download/[^"\\s]+\\.apk)"[^>]*>')
            download_matches = download_regex.finditer(html)
            
            download_links = []
            for match in download_matches:
                download_url = "https://github.com" + match.group(1)
                download_links.append(download_url)
                print(f"✓ 找到下载链接: {download_url}")
                # 从文件名中提取版本号
                file_name = download_url.split('/')[-1]
                version_match = re.search(r'v?([0-9]+\\.[0-9]+\\.[0-9]+)', file_name)
                if version_match:
                    print(f"✓ 从文件名提取的版本号: {version_match.group(1)}")
            
            if not download_links:
                print("✗ 未找到下载链接")
            else:
                print("✓ HTML 解析成功")
        except Exception as e:
            print(f"✗ HTML 解析异常: {e}")

if __name__ == "__main__":
    test_github_api()
    test_html_parse()
    test_full_flow()
