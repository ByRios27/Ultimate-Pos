import re

with open('app/src/main/java/com/example/ui/components/PosComponents.kt', 'r') as f:
    content = f.read()

# 1. Remove Text(text = draw.icon.ifBlank { "🎲" }, ...)
content = re.sub(r'Text\(\s*text\s*=\s*draw\.icon\.ifBlank\s*\{\s*"[^"]+"\s*\}[^\)]+\)\n?', '', content)

# 2. Remove ${item.draw.icon}
content = content.replace('${item.draw.icon} ', '')

# 3. Remove ${draw.icon}
content = content.replace('${draw.icon} ', '')

with open('app/src/main/java/com/example/ui/components/PosComponents.kt', 'w') as f:
    f.write(content)
