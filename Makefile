##
#
#
# @file
# @version 0.1
.PHONY: finished.html

finished.html:
	lein run template.html %background% smolsky.png %plane% smolplane.png > finished.html

# end
