mkdir build
del build\program.hex

cgs.exe -I build\program.hex -A 0xFF000000 -B firmware\20732_EEPROM.btp -D drivers -O DLConfigBD_ADDRBase:%1 firmware\A_20732A0-mint-rom-ram-spar.cgs

Chipload.exe -BLUETOOLMODE -PORT %2 -BAUDRATE 115200nfc -MINIDRIVER drivers\uart_64bytes_DISABLE_EEPROM_WP_PIN1.hex -CONFIG build\program.hex -BTP firmware\20732_EEPROM.btp -NODLMINIDRIVER