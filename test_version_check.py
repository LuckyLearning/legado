import re
import requests

# 模拟 GitHub Releases 页面的 HTML 内容
def get_github_release_page():
    url = "https://github.com/LuckyLearning/legado/releases/latest"
    try:
        response = requests.get(url, allow_redirects=True)
        response.raise_for_status()
        return response.text
    except Exception as e:
        print(f"获取页面失败: {e}")
        return ""

# 测试正则表达式
def test_regex():
    html = get_github_release_page()
    if not html:
        print("无法获取页面内容")
        return
    
    # 打印页面前 1000 个字符，查看页面结构
    print("=== 页面前 1000 个字符 ===")
    print(html[:1000])
    print("\n=== 测试版本号正则 ===")
    
    # 尝试不同的版本号正则表达式
    version_regex1 = re.compile(r'<span class="text-bold">([^<]+)</span>')
    version_regex2 = re.compile(r'version ([0-9\.]+)')
    version_regex3 = re.compile(r'([0-9\.]+)')
    
    version_match1 = version_regex1.search(html)
    version_match2 = version_regex2.search(html)
    version_match3 = version_regex3.search(html)
    
    if version_match1:
        print(f"正则1找到版本号: {version_match1.group(1)}")
    elif version_match2:
        print(f"正则2找到版本号: {version_match2.group(1)}")
    elif version_match3:
        print(f"正则3找到版本号: {version_match3.group(1)}")
    else:
        print("未找到版本号")
    
    print("\n=== 测试下载链接正则 ===")
    # 尝试不同的下载链接正则表达式
    download_regex1 = re.compile(r'<a href="(/LuckyLearning/legado/releases/download/[^"]+\.apk)"[^>]*>')
    download_regex2 = re.compile(r'href="([^"\s]+\.apk)"')
    
    download_matches1 = download_regex1.finditer(html)
    download_matches2 = download_regex2.finditer(html)
    
    download_links = []
    for match in download_matches1:
        download_url = "https://github.com" + match.group(1)
        download_links.append(download_url)
        print(f"正则1找到下载链接: {download_url}")
    
    for match in download_matches2:
        download_url = match.group(1)
        if not download_url.startswith("http"):
            download_url = "https://github.com" + download_url
        if download_url not in download_links:
            download_links.append(download_url)
            print(f"正则2找到下载链接: {download_url}")
    
    if not download_links:
        print("未找到下载链接")

if __name__ == "__main__":
    test_regex()
