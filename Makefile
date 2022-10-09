##
#
#
# @file
# @version 0.1
.PHONY: finished.html smolsky.png smolplane.png

finished.html: smolplane.png smolsky.png
	lein run template.html %background% smolsky.png %plane% smolplane.png > finished.html

smolsky.png:
	convert sky.png +dither -gravity South -resize 80 -chop 0x35 -blur 10 -posterize 12 smolsky.png

smolplane.png:
	convert plane.png +dither  -colors 15 smolplane.png
