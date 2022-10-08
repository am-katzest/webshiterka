##
#
#
# @file
# @version 0.1
.PHONY: finished.html smolsky.png smolplane.png

finished.html: smolplane.png smolsky.png
	lein run template.html %background% smolsky.png %plane% smolplane.png > finished.html

smolsky.png:
	convert sky.png +dither -gravity South -chop 0x25 -blur 10 -posterize 15 smolsky.png

smolplane.png:
	convert plane.png +dither  -resize 75% -posterize 10 smolplane.png
