include.module( 'tool-directions', [ 'smk', 'tool', 'widgets', 'tool-directions.panel-directions-html', 'tool-directions.address-search-html' ], function ( inc ) {

    var request

    function findRoute( points, option ) {
        if ( request )
            request.abort()

        var query = {
            points:     points.map( function ( w ) { return w.longitude + ',' + w.latitude } ).join( ',' ),
            outputSRS:  4326,
            criteria:   option.criteria,
            roundTrip:  option.roundTrip
        }

        return SMK.UTIL.makePromise( function ( res, rej ) {
            ( request = $.ajax( {
                timeout:    10 * 1000,
                dataType:   'json',
                url:        'https://routerdlv.api.gov.bc.ca/' + ( option.optimal ? 'optimalDirections' : 'directions' ) + '.json',
                data:       query,
                headers: {
                    apikey: 'ndLv6oEJN4z5FwwhDyaNoF4NfnYWXwVJ'
                }
            } ) ).then( res, rej )
        } )
        .then( function ( data ) {
            if ( !data.routeFound ) throw new Error( 'failed to find route' )

            return data
        } )
    }
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    Vue.component( 'address-search', {
        template: inc[ 'tool-directions.address-search-html' ],
        props: [ 'value', 'placeholder' ],
        data: function () {
            return {
                search: this.value,
                list: null,
                selectedIndex: null,
                expanded: false
            }
        },
        watch: {
            value: function ( val ) {
                this.search = this.value
            }
        },
        methods: {
            onChange: function () {
                var self = this

                this.$emit( 'input', this.search )
                this.$emit( 'update', { location: null, description: this.search } )

                this.list = null

                var query = {
                    ver:            1.2,
                    maxResults:     20,
                    outputSRS:      4326,
                    addressString:  this.search,
                    autoComplete:   true
                }

                return SMK.UTIL.makePromise( function ( res, rej ) {
                    $.ajax( {
                        timeout:    10 * 1000,
                        dataType:   'jsonp',
                        url:        'https://apps.gov.bc.ca/pub/geocoder/addresses.geojsonp',
                        data:       query,
                    } ).then( res, rej )
                } )
                .then( function ( data ) {
                    self.list = $.map( data.features, function ( feature ) {
                        if ( !feature.geometry.coordinates ) return;

                        // exclude whole province match
                        if ( feature.properties.fullAddress == 'BC' ) return;

                        return {
                            location: { longitude: feature.geometry.coordinates[ 0 ], latitude: feature.geometry.coordinates[ 1 ] },
                            description: feature.properties.fullAddress
                        }
                    } )

                    self.expanded = self.list.length > 0
                    self.selectedIndex = self.list.length > 0 ? 0 : null
                } )
            },

            onArrowDown: function () {
                if ( !this.expanded && this.list ) {
                    this.expanded = true
                    this.selectedIndex = 0
                    return
                }
                this.selectedIndex = ( ( this.selectedIndex || 0 ) + 1 ) % this.list.length
            },

            onArrowUp: function () {
                if ( !this.expanded ) return
                this.selectedIndex = ( ( this.selectedIndex || 0 ) + this.list.length - 1 ) % this.list.length
            },

            onEnter: function () {
                if ( !this.expanded ) return
                this.search = this.list[ this.selectedIndex ].description
                this.expanded = false
                this.$emit( 'update', this.list[ this.selectedIndex ] )
            },

            handleClickOutside( ev ) {
                if ( this.$el.contains( ev.target ) ) return

                this.expanded = false
                this.selectedIndex = null
            }
        },
        mounted() {
            document.addEventListener( 'click', this.handleClickOutside )
        },
        destroyed() {
            document.removeEventListener( 'click', this.handleClickOutside )
        }
    } )
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    Vue.component( 'directions-widget', {
        extends: inc.widgets.toolButton,
    } )

    Vue.component( 'directions-panel', {
        template: inc[ 'tool-directions.panel-directions-html' ],
        props: [ 'busy', 'waypoints', 'directions', 'directionHighlight', 'directionPick', 'summary' ],
        data: function () {
            return {
                optimal:    false,
                roundTrip:  false,
                criteria:   'shortest'
            }
        },
        computed: {
            firstWaypointIndex: function () {
                for ( var i = 0; i < this.waypoints.length; i++ )
                    if ( this.waypoints[ i ].location )
                        return i
            },
            lastWaypointIndex: function () {
                for ( var i = this.waypoints.length - 1; i >= 0 ; i-- )
                    if ( this.waypoints[ i ].location )
                        return i
            },
        },
    } )
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    function DirectionsTool( option ) {
        this.makePropWidget( 'icon', 'directions_car' )

        this.makePropPanel( 'busy', false )
        this.makePropPanel( 'waypoints', [] )
        this.makePropPanel( 'directions', [] )
        this.makePropPanel( 'directionHighlight', null )
        this.makePropPanel( 'directionPick', null )
        this.makePropPanel( 'summary', null )

        SMK.TYPE.Tool.prototype.constructor.call( this, $.extend( {
            order:          4,
            title:          'Directions',
            widgetComponent:'directions-widget',
            panelComponent: 'directions-panel',
        }, option ) )

        this.routeOption = {
            optimal:    false,
            roundTrip:  false,
            criteria:   'shortest'
        }
    }

    SMK.TYPE.DirectionsTool = DirectionsTool

    $.extend( DirectionsTool.prototype, SMK.TYPE.Tool.prototype )
    DirectionsTool.prototype.afterInitialize = []
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    DirectionsTool.prototype.afterInitialize.push( function ( smk, aux ) {
        var self = this

        this.changedActive( function () {
            if ( self.active ) {
                if ( self.waypoints.length == 0 ) {
                    self.addWaypointCurrentLocation().then( function () {
                        self.addWaypoint()
                        self.displayWaypoints()
                    } )
                }
                else {
                    self.findRoute()
                }
            }
        } )

        this.getCurrentLocation = function () {
            return smk.$viewer.getCurrentLocation().then( function ( loc ) {
                return smk.$viewer.findNearestSite( loc ).then( function ( site ) {
                    return { location: loc, description: site.fullAddress }
                } )
            } )
        }

        smk.$viewer.handlePick( this, function ( location ) {
            return smk.$viewer.findNearestSite( location.map ).then( function ( site ) {
                var empty = self.waypoints.find( function ( w ) { return !w.location } )

                if ( !empty )
                    throw new Error( 'shouldnt happen' )

                empty.description = site.fullAddress
                empty.location = location.map
                self.addWaypoint()

                self.findRoute()
            } )
        } )

        aux.widget.vm.$on( 'directions-widget.click', function () {
            if ( !self.visible || !self.enabled ) return

            self.active = !self.active
        } )

        aux.panel.vm.$on( 'directions-panel.option', function ( ev, comp ) {
            self.routeOption.optimal = comp.optimal
            self.routeOption.roundTrip = comp.roundTrip
            self.routeOption.criteria = comp.criteria

            self.findRoute()
        } )

        aux.panel.vm.$on( 'directions-panel.reverse', function ( ev ) {
            self.waypoints.reverse()
            self.findRoute()
        } )

        aux.panel.vm.$on( 'directions-panel.clear', function ( ev ) {
            self.resetWaypoints()
            self.addWaypointCurrentLocation().then( function () {
                self.addWaypoint()
                self.displayWaypoints()
            } )
        } )

        aux.panel.vm.$on( 'directions-panel.hover-direction', function ( ev ) {
            self.directionHighlight = ev.highlight
        } )

        aux.panel.vm.$on( 'directions-panel.pick-direction', function ( ev ) {
            self.directionPick = ev.pick
        } )

        aux.panel.vm.$on( 'directions-panel.changed-waypoints', function ( ev ) {
            self.findRoute()
        } )

        aux.panel.vm.$on( 'directions-panel.zoom-waypoint', function ( ev ) {
        } )

        aux.panel.vm.$on( 'directions-panel.remove-waypoint', function ( ev ) {
            self.waypoints.splice( ev.index, 1 )

            self.findRoute()
        } )

        aux.panel.vm.$on( 'directions-panel.update-waypoint', function ( ev ) {
            var empty = self.waypoints.findIndex( function ( w ) { return !w.location } )

            self.waypoints[ ev.index ] = ev.item

            if ( !ev.item.location && ev.index != empty )
                self.waypoints.splice( empty, 1 )

            if ( ev.item.location && ev.index == empty )
                self.addWaypoint()

            self.findRoute()
        } )

    } )
    // _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _ _
    //
    DirectionsTool.prototype.addWaypoint = function ( location, description ) {
        var wp = { location: null, description: null }

        if ( location )
            wp.location = { latitude: location.latitude, longitude: location.longitude }

        wp.description = description

        this.waypoints.push( wp )
    }

    DirectionsTool.prototype.addWaypointCurrentLocation = function () {
        var self = this

        this.busy = true

        return this.getCurrentLocation()
            .then( function ( res ) {
                self.addWaypoint( res.location, '(CURRENT) ' + res.description )
            } )
            .finally( function () {
                self.busy = false
            } )
    }

    DirectionsTool.prototype.resetWaypoints = function ( ) {
        var self = this

        this.waypoints = []
        this.directions = []
        this.directionHighlight = null
        this.directionPick = null
        this.summary = null
    }

    DirectionsTool.prototype.findRoute = function () {
        var self = this

        this.directions = []
        this.summary = null
        this.displayRoute()

        var points = this.waypoints
            .map( function ( w, i ) { return { index: i, latitude: w.location && w.location.latitude, longitude: w.location && w.location.longitude } } )
            .filter( function ( w ) { return !!w.latitude } )

        if ( points.length < 2 ) {
            self.displayWaypoints()
            return
        }
        // console.log( points )

        this.busy = true

        findRoute( points, this.routeOption ).then( function ( data ) {
            self.displayRoute( data.route )

            if ( data.visitOrder && data.visitOrder.findIndex( function ( v, i ) { return points[ v ].index != i } ) != -1 ) {
                // console.log( data.visitOrder )
                // console.log( data.visitOrder.map( function ( v ) { return points[ v ].index } ) )
                // console.log( JSON.stringify( self.waypoints, null, '  ' ) )

                self.waypoints = data.visitOrder.map( function ( v ) { return self.waypoints[ points[ v ].index ] } )
                // console.log( JSON.stringify( self.waypoints, null, '  ' ) )
                self.addWaypoint()
            }

            self.displayWaypoints()

            self.summary = 'Route travels ' + data.distance + ' km in ' + data.timeText

            self.directions = data.directions.map( function ( dir ) {
                dir.instruction = dir.text.replace( /\sfor\s(\d+.?\d*\sk?m)\s[(](\d+).+?((\d+).+)?$/, function ( m, a, b, c, d ) {
                    dir.distance = a
                    if ( d )
                        dir.time = ( '0' + b ).substr( -2 ) + ':' + ( '0' + d ).substr( -2 )
                    else
                        dir.time = '00:' + ( '0' + b ).substr( -2 )

                    return ''
                } )
                return dir
            } )
        } )
        .finally( function () {
            self.busy = false
        } )
    }

    return DirectionsTool
} )

