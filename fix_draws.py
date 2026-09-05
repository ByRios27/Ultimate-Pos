with open('app/src/main/java/com/example/ui/screens/draws/DrawsScreen.kt', 'r') as f:
    lines = f.readlines()

new_lines = []
skip = False
for i, line in enumerate(lines):
    if 'var icon by remember' in line:
        continue
    if 'OutlinedTextField(' in line and 'value = icon,' in lines[i+1]:
        skip = True
        continue
    if skip and ')' in line and 'var expandedDigits' in lines[i+2]: # simplistic check
        pass
    # Actually it's easier to just use sed or Python regex.
