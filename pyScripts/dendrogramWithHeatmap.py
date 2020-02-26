import pandas as pd
import plotly
import argparse
import plotly.figure_factory as FF
from plotly.graph_objs import *
import scipy

parser = argparse.ArgumentParser()
parser.add_argument("-s", type=str, default=[], nargs='+', help="sample name")
parser.add_argument("-rd", type=str, default="euclidean", help="row dist method")
parser.add_argument("-cd", type=str, default="euclidean", help="col dist method")
parser.add_argument("-rl", type=str, default="complete", help="row linkage method")
parser.add_argument("-cl", type=str, default="complete", help="col linkage method")
args = parser.parse_args()

df = pd.read_csv("deal.txt", sep='\t', index_col=0)
geneDf = pd.read_csv("gene.txt", sep='\t',header=None)

g = geneDf[0]
df = df.ix[g]
if args.s:
    df = df[args.s]
df.to_csv("deal.txt", sep="\t")
tDf = df.T
cdist = lambda x: scipy.spatial.distance.pdist(x, metric=args.cd)
cLinkage = lambda x: scipy.cluster.hierarchy.linkage(x, args.cl)
figure = FF.create_dendrogram(tDf, orientation='bottom', labels=tDf.index.values, distfun=cdist, linkagefun=cLinkage)
for i in range(len(figure['data'])):
    figure['data'][i]['yaxis'] = 'y2'
figure["data"].update({"hoverinfo": "x+y"})
rdist = lambda x: scipy.spatial.distance.pdist(x, metric=args.rd)
rLinkage = lambda x: scipy.cluster.hierarchy.linkage(x, args.rl)
dendro_side = FF.create_dendrogram(df, orientation='left', labels=df.index.values, distfun=rdist, linkagefun=rLinkage)
for i in range(len(dendro_side['data'])):
    dendro_side['data'][i]['xaxis'] = 'x2'
dendro_side["data"].update({"hoverinfo": "x+y"})
figure['data'].extend(dendro_side['data'])
figure['layout']['yaxis']['tickvals'] = dendro_side['layout']['yaxis']['tickvals']
figure['layout']['yaxis']['ticktext'] = dendro_side['layout']['yaxis']['ticktext']
cols = figure['layout']['xaxis']['ticktext']
rows = figure['layout']['yaxis']['ticktext']
hDf = df[cols]
hDf = hDf.ix[rows]
heatmap = Data([
    Heatmap(
        x=hDf.columns,
        y=hDf.index,
        z=hDf.values.tolist(),
    )
])
heatmap[0]['x'] = figure['layout']['xaxis']['tickvals']
heatmap[0]['y'] = dendro_side['layout']['yaxis']['tickvals']
heatmap[0].update({"hoverinfo": "x+y+z"})
figure['data'].extend(Data(heatmap))
figure['layout'].update({'width': 700, 'height': 800,
                         'showlegend': False, 'hovermode': 'closest', "margin": {
        "l": 240,
    }, "title": "Heatmap",
                         })
figure['layout']['xaxis'].update({'domain': [0, 0.85],
                                  'mirror': False,
                                  'showgrid': False,
                                  'showline': False,
                                  'zeroline': False,
                                  'showticklabels': True,
                                  })
figure['layout'].update({'xaxis2': {'domain': [0.85, 1],
                                    'mirror': False,
                                    'showgrid': False,
                                    'showline': False,
                                    'zeroline': False,
                                    'showticklabels': False,
                                    'ticks': ""}})
figure['layout']['yaxis'].update({'domain': [0, .85],
                                  'mirror': False,
                                  'showgrid': False,
                                  'showline': False,
                                  'zeroline': False,
                                  'showticklabels': True,
                                  })
figure['layout'].update({'yaxis2': {'domain': [.85, 1],
                                    'mirror': False,
                                    'showgrid': False,
                                    'showline': False,
                                    'zeroline': False,
                                    'showticklabels': False,
                                    'ticks': ""}})
config = dict(
    displaylogo=False,
    showLink=False,
)
# plotly.offline.plot(fig, filename="basic-scatter.html", config=config)
a = plotly.offline.plot(figure, filename="basic-scatter.html", output_type="div", include_plotlyjs=False, config=config)
f = open('div.txt', 'w')
f.write(a)
f.close()
