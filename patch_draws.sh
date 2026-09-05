sed -i '489,567c\
                    var expandedDigits by remember { mutableStateOf(false) }\n\
                    ExposedDropdownMenuBox(\n\
                        expanded = expandedDigits,\n\
                        onExpandedChange = { expandedDigits = !expandedDigits },\n\
                        modifier = Modifier.weight(1.2f)\n\
                    ) {\n\
                        OutlinedTextField(\n\
                            value = allowedDigits.split(",").maxOrNull()?.plus(" cifras") ?: "4 cifras",\n\
                            onValueChange = {},\n\
                            readOnly = true,\n\
                            label = { Text("Cifras") },\n\
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDigits) },\n\
                            modifier = Modifier.fillMaxWidth().menuAnchor(),\n\
                            shape = RoundedCornerShape(10.dp),\n\
                            colors = OutlinedTextFieldDefaults.colors(\n\
                                focusedContainerColor = PosPanel,\n\
                                unfocusedContainerColor = PosPanel,\n\
                                focusedBorderColor = PosGreenAction,\n\
                                unfocusedBorderColor = PosBorder,\n\
                                focusedTextColor = PosTextPrimary,\n\
                                unfocusedTextColor = PosTextPrimary\n\
                            )\n\
                        )\n\
                        ExposedDropdownMenu(\n\
                            expanded = expandedDigits,\n\
                            onDismissRequest = { expandedDigits = false }\n\
                        ) {\n\
                            (2..6).forEach { num ->\n\
                                DropdownMenuItem(\n\
                                    text = { Text("$num cifras") },\n\
                                    onClick = {\n\
                                        allowedDigits = (1..num).joinToString(",")\n\
                                        expandedDigits = false\n\
                                    }\n\
                                )\n\
                            }\n\
                        }\n\
                    }\n\
                }' app/src/main/java/com/example/ui/screens/draws/DrawsScreen.kt
