# FTPClient - Simple FTP, FTPS and SFTP client for Android

[![CI status](https://ci.codeberg.org/api/badges/7694/status.svg)](https://ci.codeberg.org/repos/7694)
[![Translation status](https://translate.codeberg.org/widget/ftpclient/svg-badge.svg)](https://translate.codeberg.org/engage/ftpclient/)

FTPClient is a very simple FTP, FPTS and SFTP client for Android 5.0+,
using [Apache Commons Net](https://commons.apache.org/proper/commons-net/) for FTP and FTPS
and [SSHJ](https://github.com/hierynomus/sshj/) for SFTP.

[![Get it on F-Droid](https://codeberg.org/qwerty287/ftpclient/raw/branch/main/assets/f-droid.png)](https://f-droid.org/packages/de.qwerty287.ftpclient/)
[![Get it on Codeberg](https://codeberg.org/qwerty287/ftpclient/raw/branch/main/assets/codeberg.png)](https://codeberg.org/qwerty287/ftpclient/releases/latest)

## Features

* view directory content
* upload files to FTP server
* create directories
* download files
* view file and directory properties
* rename files and directories
* view text and image files
* edit text files

## Screenshots

<img src="https://codeberg.org/qwerty287/ftpclient/raw/branch/main/fastlane/metadata/android/en-US/images/phoneScreenshots/001.png" width="300">
<img src="https://codeberg.org/qwerty287/ftpclient/raw/branch/main/fastlane/metadata/android/en-US/images/phoneScreenshots/002.png" width="300">
<img src="https://codeberg.org/qwerty287/ftpclient/raw/branch/main/fastlane/metadata/android/en-US/images/phoneScreenshots/003.png" width="300">
<img src="https://codeberg.org/qwerty287/ftpclient/raw/branch/main/fastlane/metadata/android/en-US/images/phoneScreenshots/004.png" width="300">

## Troubleshooting

Before opening an issue, please take a look at the following information which may help to fix your issue.

### FTPS

If FPTClient can't connect to your server or does not view any files, please try toggling the "Use passive mode" and
"Use private data connection" checkboxes, also in any combination.
