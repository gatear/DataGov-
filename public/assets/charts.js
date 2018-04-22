(function (){

    var routes = jsRoutes.controllers.Application;


    var arrests = document.querySelector('#arrests').getContext('2d');
    var domestics = document.querySelector('#domestics').getContext('2d');
    var topCrimes = document.querySelector('#top-crimes').getContext('2d');


    var chartOptions = {
        legend: {
            display: true,
            position: 'bottom'
        },

    };

    $.get( routes.setPieChart().url, function () { }).done( function(data){

        //Arrests pie-chart
        new Chart( arrests, {
            type:'pie',
            responsive: true,
            data: {
                datasets: [{
                    label: 'From 2001-2017',


                    data : [
                        ((data.arrests / data.crimes) * 100).toFixed(2),
                        ((data.crimes  - data.arrests)/ data.crimes * 100).toFixed(2)
                        ],
                    backgroundColor : [ '#E20074','#4c516d'],
                }],

                labels:[
                    'With Arrest [%]',
                    'Without Arrest [%]'
                ]
            },
            options: chartOptions
        });

        //Domestic pie-chart
        new Chart( domestics, {
            type:'pie',
            responsive: true,
            data: {
                datasets: [{
                    label: 'From 2001-2017',

                    data : [
                        ((data.domestics / data.crimes) * 100).toFixed(2),
                        ((data.crimes  - data.domestics)/ data.crimes * 100).toFixed(2)
                    ],

                    backgroundColor : [ '#E20074','#4c516d']

                }],

                labels:[
                    'Domestics Crimes [%]',
                    'Other Types [%]'
                ]
            },
            options: chartOptions
        });

    });

    //The argument is the limit of incidents
    $.get( routes.setLineChart( 500000 ).url, function () { }).done( function(data){

        let colors = ['red','blue','purple','red','orange'];

        let response = data.timeseries.map( object => ({ data:  object.data.map(tuple => tuple.count),
                                                         label: object.type,
                                                         borderColor: colors.pop(),
                                                         fill : false,
                                                         pointRadius: 6,
                                                         pointHoverRadius: 7,}) );

        console.log(response);

        new Chart( topCrimes, {
            type: 'line',

            responsive: true,

            data: {
                //years
                labels: [2001, 2002, 2003, 2004, 2005, 2006, 2007, 2008, 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017],

                //parsed response
                datasets:   response
            },

            options: {
                scales: {
                    xAxes: [{
                        scaleLabel: {
                            display: true,
                            labelString: 'YEAR'
                        }
                    }],
                    yAxes:[{
                        scaleLabel: {
                            display: true,
                            labelString: 'EVENT COUNT'
                        }
                    }]
                }
            }

        });
        });


    // var topCrimesLine =  new Chart( topCrimes, {
    //     type: 'line',
    //     data: { },
    //     options: chartOptions
    // });
})();

