/*
 Description: Renders the asset.jag view
 Filename:asset.js
 Created Date: 29/7/2013
 */
var render=function(theme,data,meta,require){

    var log = new Log();

    //var _url = "/publisher/asset/"  + data.meta.shortName + "/" + data.info.id + "/edit"
    var listPartial='view-asset';

    //Determine what view to show
    switch(data.op){

        case 'create':
            listPartial='add-asset';
            break;
        case 'view':
            data = require('/helpers/view-asset.js').merge(data);
            listPartial='view-asset';
            break;
        case 'edit':
            data = require('/helpers/edit-asset.js').processData(data);
            listPartial='edit-asset';
            break;
        case 'lifecycle':
            listPartial='lifecycle-asset';
            break;
        case 'versions':
            listPartial='versions-asset';
            break;
        case 'documentation':
            listPartial='documentation';
            break;
        case 'copyapp':
            data = require('/helpers/copy-app.js').processData(data);
            listPartial='copy-app';
            break;
        default:
            break;
    }

    var copyOfData = parse(stringify(data));
    data.newViewData =  require('/helpers/splitter.js').splitData(copyOfData);

    theme('single-col-fluid', {
        title: data.title,
        header: [
            {
                partial: 'header',
                context: data
            }
        ],
        ribbon: [
            {
                partial: 'ribbon',
                context: {active:listPartial}
            }
        ],
        leftnav: [
            {
                partial: 'left-nav',
                context: require('/helpers/left-nav.js').generateLeftNavJson(data, listPartial)
            }
        ],
        listassets: [
            {
                partial:listPartial,
                context: data
            }
        ],
        heading: [
            {
                partial: 'heading',
                context: {title: "", menuItems: require('/helpers/left-nav.js').generateLeftNavJson(data, listPartial)}
            }
        ]
    });
};
