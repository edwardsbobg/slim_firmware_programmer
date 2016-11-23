mkdir build
del build\program.hex

cgs.exe -I build\program.hex -A 0xFF000000 -B firmware\20732_EEPROM.btp -D drivers -O DLConfigBD_ADDRBase:%1 firmware\A_20732A0-mint-rom-ram-spar.cgs