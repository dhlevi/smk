include.module( 'tool-minimap', [ 'tool' ], function () {

    function MinimapTool( option ) {
        SMK.TYPE.Tool.prototype.constructor.call( this, $.extend( {
        }, option ) )
    }

    SMK.TYPE.MinimapTool = MinimapTool

    $.extend( MinimapTool.prototype, SMK.TYPE.Tool.prototype )
    MinimapTool.prototype.afterInitialize = []
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    return MinimapTool

} )

