import plotly.graph_objs as go
from sklearn.metrics import roc_curve, auc
import plotly
import pandas as pd
import argparse

parser = argparse.ArgumentParser()
args = parser.parse_args()
df = pd.read_csv("deal.txt", sep='\t', index_col=0)
tDf = df.T
y_test = tDf["FLAG"]
data = []
geneDf = pd.read_csv("gene.txt", sep='\t',header=None)
gs = geneDf[0]
for g in gs:
    probas = tDf[g]
    fpr, tpr, thresholds = roc_curve(y_test, probas, pos_label=1)
    roc_auc = auc(fpr, tpr)
    lw = 2
    trace1 = go.Scatter(
        x=fpr,
        y=tpr,
        mode='lines',
        line=dict(width=lw),
        name=g + ' (area = %0.3f)' % roc_auc,
        text=g,
        hoverinfo='x+y+text',
    )
    data.append(trace1)

trace2 = go.Scatter(x=[0, 1], y=[0, 1],
                    mode='lines',
                    line=dict(color='navy', width=lw, dash='dash'),
                    showlegend=False,
                    )
data.append(trace2)

layout = go.Layout(title='ROC curve',
                   xaxis=dict(
                       title='False Positive Rate',
                       showgrid=False,
                   ),
                   yaxis=dict(
                       title='True Positive Rate',
                       showgrid=False,
                   ),
                   hovermode="closest",
                   width=900,
                   height=500

                   )
config = dict(
    displaylogo=False,
    showLink=False,
)

fig = go.Figure(data=data, layout=layout)
a = plotly.offline.plot(fig, filename="basic-scatter.html", config=config, output_type="div", include_plotlyjs=False)
f = open('div.txt', 'w')
f.write(a)
f.close()
