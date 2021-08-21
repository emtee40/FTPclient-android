# Use this script in any subdirectory of /usr/share/mime/, except application/
import os

files = os.listdir(".")
mimetypesList = []
for i in files:
    file = open(i, "r")
    lines = file.readlines()
    file.close()
    for line in lines:
        if line.strip().startswith("<glob pattern="):
            patternStart = line.find('.') + 1
            line = line[patternStart:]
            patternEnd = line.find('"')
            pattern = line[:patternEnd]
            mimetypesList.append(pattern)

for i in mimetypesList:
    print('"' + i + '"', end=", ")

print("")
