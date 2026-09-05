import re

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'r') as f:
    content = f.read()

content = content.replace('_activeDraws.value', 'activeDraws.value')

with open('app/src/main/java/com/example/ui/viewmodel/PosViewModel.kt', 'w') as f:
    f.write(content)
