import re

with open('app/src/main/java/com/example/util/ThermalReceiptHelper.kt', 'r') as f:
    content = f.read()

# We need to insert the extension function at the top of the file but after imports.
extension = """
fun String.removeEmojis(): String {
    return this.replace(Regex("[^\\\\p{L}\\\\p{M}\\\\p{N}\\\\p{P}\\\\p{Z}\\\\s]"), "").trim()
}
"""
# find the first `object ThermalReceiptHelper` or `class`
insert_idx = content.find('object ThermalReceiptHelper')
content = content[:insert_idx] + extension + "\n" + content[insert_idx:]

# In generateEscPos58mm
# replace addString(">> $drawName <<\n") with addString(">> ${drawName.removeEmojis()} <<\n")
content = content.replace('addString(">> $drawName <<\\n")', 'addString(">> ${drawName.removeEmojis()} <<\\n")')

# In printTicket58mm
# replace canvas.drawText(">> ${firstItem.drawName} <<", ... ) with canvas.drawText(">> ${firstItem.drawName.removeEmojis()} <<", ...)
content = content.replace('canvas.drawText(">> ${firstItem.drawName} <<"', 'canvas.drawText(">> ${firstItem.drawName.removeEmojis()} <<"')

with open('app/src/main/java/com/example/util/ThermalReceiptHelper.kt', 'w') as f:
    f.write(content)
