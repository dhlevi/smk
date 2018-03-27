include.module( 'tool-query', [ 'smk', 'tool', 'widgets', 'tool-query.panel-query-html', 'tool-query.parameter-input-html', 'tool-query.parameter-select-html', 'tool-query.parameter-constant-html' ], function ( inc ) {

    Vue.component( 'parameter-constant', {
        template: inc[ 'tool-query.parameter-constant-html' ],
        props: [ 'title', 'value' ],
    } )

    Vue.component( 'parameter-input', {
        template: inc[ 'tool-query.parameter-input-html' ],
        props: [ 'title', 'value' ],
        data: function () {
            return {
                input: this.value
            }
        },
        watch: {
            value: function ( val ) {
                this.input = val
            }
        }
    } )

    Vue.component( 'parameter-select', {
        template: inc[ 'tool-query.parameter-select-html' ],
        props: [ 'title', 'choices', 'value' ],
        data: function () {
            // console.log( 'data', this.value )
            return {
                selected: this.value
            }
        },
        watch: {
            value: function ( val ) {
                // console.log( 'watch', val )
                this.selected = val
            }
        }
    } )
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    Vue.component( 'query-widget', {
        extends: inc.widgets.toolButton,
    } )

    Vue.component( 'query-panel', {
        template: inc[ 'tool-query.panel-query-html' ],
        props: [ 'busy', 'queries', 'description', 'parameters', 'layerTitle', 'features', 'highlightId', 'pickId' ],
        methods: {
            isEmpty: function () {
                return !this.parameters
            }
        },
    } )
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    function QueryTool( option ) {
        this.makePropWidget( 'icon', 'question_answer' )

        this.makePropPanel( 'busy', false )
        this.makePropPanel( 'queries', [] )
        this.makePropPanel( 'description', null )
        this.makePropPanel( 'parameters', null )
        this.makePropPanel( 'layerTitle', null )
        this.makePropPanel( 'features', null )
        this.makePropPanel( 'highlightId', null )
        this.makePropPanel( 'pickId', null )

        SMK.TYPE.Tool.prototype.constructor.call( this, $.extend( {
            order:          4,
            title:          'Query',
            widgetComponent:'query-widget',
            panelComponent: 'query-panel',
        }, option ) )
    }

    SMK.TYPE.QueryTool = QueryTool

    $.extend( QueryTool.prototype, SMK.TYPE.Tool.prototype )
    QueryTool.prototype.afterInitialize = []
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    QueryTool.prototype.afterInitialize.push( function ( smk, aux ) {
        var self = this

        // smk.$viewer.handlePick( this, function ( location ) {
        //     smk.$viewer.identifyFeatures( location )
        // } )

        this.queries = Object.values( smk.$viewer.query ).map( function ( q ) {
            return {
                id: q.id,
                title: q.title
            }
        } )

        aux.widget.vm.$on( 'query-widget.click', function () {
            if ( !self.visible || !self.enabled ) return

            self.active = !self.active
        } )

        aux.panel.vm.$on( 'query-panel.select-query', function ( ev ) {
            self.selectedQueryId = ev.id
            self.layerTitle = smk.$viewer.query[ ev.id ].layer.config.title
            self.description = smk.$viewer.query[ ev.id ].description
            self.parameters = smk.$viewer.query[ ev.id ].getParameters()
        } )

        aux.panel.vm.$on( 'query-panel.parameter-input', function ( ev ) {
            // console.log( ev )
            self.parameters[ ev.index ].prop.value = ev.value
        } )


        // aux.panel.vm.$on( 'query-panel.hover', function ( ev ) {
        //     smk.$viewer.identified.highlight( ev.features && ev.features.map( function ( f ) { return f.id } ) )
        // } )

        // aux.panel.vm.$on( 'query-panel.add-all', function ( ev ) {
        // } )

        aux.panel.vm.$on( 'query-panel.clear', function ( ev ) {
            smk.$viewer.queried[ self.selectedQueryId ].clear()

            self.parameters.forEach( function ( p, i ) {
                p.prop.value = smk.$viewer.query[ self.selectedQueryId ].parameters[ i ].value
            } )
        } )

        aux.panel.vm.$on( 'query-panel.execute', function ( ev ) {
            smk.$viewer.queried[ self.selectedQueryId ].clear()
            self.busy = true

            var param = {}
            self.parameters.forEach( function ( p, i ) {
                param[ p.prop.id ] = $.extend( {}, p.prop )
            } )

            return smk.$viewer.query[ self.selectedQueryId ].queryLayer( param )
                .finally( function () {
                    self.busy = false
                } )
        } )

        // smk.$viewer.pickedLocation( function () {
        //     var enabledTools = Object.values( smk.$tool ).filter( function ( t ) { t.enabled } )


        // } )

        // smk.$viewer.startedIdentify( function ( ev ) {
        //     self.busy = true
        // } )

        // smk.$viewer.finishedIdentify( function ( ev ) {
        //     self.busy = false

        //     if ( self.active && smk.$viewer.identified.isEmpty() )
        //         self.active = false
        // } )

        // smk.$viewer.identified.addedFeatures( function ( ev ) {
        //     self.active = true
        //     // sb.vm.$emit( 'activate-tool', { active: true, id: 'identify' } )

        //     var ly = smk.$viewer.layerId[ ev.layerId ]

        //     self.layers[ ly.index ] = {
        //         id: ly.config.id,
        //         title: ly.config.title,
        //         features: ev.features
        //     }
        // } )

        // smk.$viewer.identified.removedFeatures( function ( ev ) {
        // } )

        // smk.$viewer.identified.pickedFeature( function ( ev ) {
        //     self.highlightId = ev.feature && ev.feature.id
        // } )

        // smk.$viewer.identified.highlightedFeatures( function ( ev ) {
        // } )

        // smk.$viewer.identified.clearedFeatures( function ( ev ) {
        //     self.layers = []
        // } )

        // var el = smk.addToContainer( inc[ 'tool-query.popup-identify-html' ] )

        // var popupModel = {
        //     feature: null,
        //     layer: null
        // }

        // this.popupVm = new Vue( {
        //     el: el,
        //     data: popupModel,
        //     methods: {
        //         debug: function ( x ) {
        //             console.log( arguments )
        //             return x
        //         },
        //         zoomToFeature: function ( layer, feature ) {
        //             return smk.$viewer.zoomToFeature( layer, feature )
        //         },
        //         directionsToFeature: function ( layer, feature ) {
        //             return smk.$viewer.directionsToFeature( layer, feature )
        //         },
        //         selectFeature: function ( layer, feature ) {
        //             smk.$viewer.selected.add( layer.config.id, [ feature ] )
        //         }
        //     }
        // } )

        // smk.$viewer.identified.pickedFeature( function ( ev ) {
        //     if ( !ev.feature ) return

        //     popupModel.feature = ev.feature
        //     popupModel.layer = smk.$viewer.layerId[ ev.feature.layerId ]
        // } )

        // smk.$viewer.getIdentifyPopupEl = function () { return self.popupVm.$el }
    } )

    return QueryTool
} )
