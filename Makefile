##
#
#
# @file
# @version 0.1
.PHONY: finished.html smolsky.png smolplane.png

finished.html: smolplane.png smolsky.png
	lein run template.html %background% smolsky.png %plane% smolplane.png > finished.html

smolsky.png:
	convert sky.png +dither -gravity South -resize 50 -chop 0x35 -blur 5 -posterize 8 smolsky.png

smolplane.png:
	convert plane.png +dither  -resize 40% -colors 13 smolplane.png
