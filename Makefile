##
#
#
# @file
# @version 0.1
.PHONY: finished.html smolsky.png smolplane.png

finished.html: smolplane.png smolsky.png
	lein run template.html %background% smolsky.png %plane% smolplane.png > finished.html

smolsky.png:
	convert sky.png +dither -gravity South -resize 80 -chop 0x50 -blur 20 -posterize 8 smolsky.png

smolplane.png:
	convert plane.png +dither  -resize 45% -colors 12 smolplane.png
