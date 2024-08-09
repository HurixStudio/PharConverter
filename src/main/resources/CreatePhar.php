<?php

/*
 *  _   _            _        ____  _             _ _
 *  | | | |_   _ _ __(_)_  __ / ___|| |_ _   _  __| (_) ___
 *  | |_| | | | | '__| \ \/ / \___ \| __| | | |/ _` | |/ _ \
 *  |  _  | |_| | |  | |>  <   ___) | |_| |_| | (_| | | (_) |
 *  |_| |_|\__,_|_|  |_/_/\_\ |____/ \__|\__,_|\__,_|_|\___/
 *
 *  Hurix is a Minecraft Bedrock teams created in 2022.
 *  @author HurixStudio
 *  @github https://github.com/HurixStudio
 *  @link https://hurix.xyz
 */

ini_set('phar.readonly', '0');

if ($argc != 3) {
	echo "Usage: php CreatePhar.php <pharFile> <directory>\n";
	exit(1);
}

$pharFile = $argv[1];
$directory = $argv[2];

if (file_exists($pharFile)) {
	if (is_file($pharFile)) {
		unlink($pharFile);
	} else {
		echo "Erreur : '$pharFile' n'est pas un fichier.\n";
		exit(1);
	}
}

try {
	if (!is_dir($directory)) {
		echo "Erreur : le répertoire '$directory' n'existe pas.\n";
		exit(1);
	}

	$phar = new Phar($pharFile);
	$phar->buildFromDirectory($directory);
	$phar->setStub($phar->createDefaultStub('index.php'));

	echo "PHAR created successfully at $pharFile\n";
} catch (Exception $e) {
	echo "Failed to create PHAR: " . $e->getMessage() . "\n";
	exit(1);
}