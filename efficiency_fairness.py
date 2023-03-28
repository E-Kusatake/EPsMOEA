from codecs import utf_8_encode
import pulp 
import statistics
import matplotlib.pyplot as plt
import inequality
import random
import csv
import pprint
import pandas as pd

# 入力パラメータ
N = 10 # Prosumer数
#c = [30,50,40,20,30] # Capacityのリスト
#d = [20,30,10,30,50] # demandのリスト
a = [[0 for i in range(10)] for j in range(10)]  #Capacityを格納するリスト
b = [[0 for i in range(10)] for j in range(10)]  #Demandを格納するリスト

c = [0] * 10
d = [0] * 10

for p in range(10): #N回分のデータセット作成
    while 1:
        c_c = 0
        p_c = 0
        c_v = 0
        p_v = 0
        for q in range(10):
            a[p][q] = random.randint(1, 10) * 10
            b[p][q] = random.randint(1, 10) * 10
            if a[p][q] > b[p][q]:
                c_c += 1
                c_v += a[p][q]
            elif a[p][q] < b[p][q]:
                p_c += 1
                p_v += b[p][q]
            else:
                while a[p][q] == b[p][q]:
                    a[p][q] = random.randint(1, 10) * 10
                if a[p][q] > b[p][q]:
                    c_c += 1
                    c_v += a[p][q]
                else:
                    p_c += 1
                    p_v += b[p][q] 
        if c_c == 0 or p_c == 0:
            continue              
        else:
        #if c_c > p_c and c_v > p_v:
            break

FAIRNESS_FACTOR_LIST = ['stdev','gini'] # Fairness評価指標の種類
PLOT_NO = 25 # 実行結果にプロットする1系列の点の数

# Fairnessを考慮した最適化問題の定義
def set_problem_opt(VS,VB,variable_dict,pq,P,Q,shrink_rate):
    # 数理モデルの宣言
    problem = pulp.LpProblem(sense=pulp.LpMaximize)

    # 共通の決定変数の定義
    problem = add_common_variables(problem,VS,VB,variable_dict)

    # 決定変数wの定義（非負変数）
    variable_dict['w'] = pulp.LpVariable('w', lowBound=0) 

    # 目的関数の定義
    problem += variable_dict['w']

    # 制約条件の定義
    ## 共通の制約条件の定義
    problem = add_common_constraints(problem,VS,VB,P,Q,pq,shrink_rate,variable_dict)

    # 変数wに関する制約条件
    ## P > Qのとき
    if P>Q:
        for i in VS:
            available_resource = min(pq[i-1], Q * shrink_rate)
            problem += variable_dict['w'] <= pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for j in VB) / available_resource
    ## Q >=Pのとき
    else:
        for j in VB:
            available_resource = min(pq[j-1], P * shrink_rate)
            problem += variable_dict['w'] <= pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for i in VS) / available_resource

    return variable_dict, problem

# Fairnessを考慮していない最適化問題の定義
def set_problem(VS,VB,variable_dict,pq,P,Q,shrink_rate):
    # 数理モデルの宣言
    problem = pulp.LpProblem(sense=pulp.LpMaximize)

    # 共通の決定変数の定義
    problem = add_common_variables(problem,VS,VB,variable_dict)

    # 目的関数の定義
    ## P > Qのとき
    if P>Q:
        for i in VS:
            problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for j in VB)
    ## Q >= Pのとき
    else:
        for j in VB:
            problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for i in VS)

    # 制約条件の定義
    ## 共通の制約条件の定義
    problem = add_common_constraints(problem,VS,VB,P,Q,pq,shrink_rate,variable_dict)

    return variable_dict, problem

# 共通の決定変数の定義
def add_common_variables(problem,VS,VB,variable_dict):
    for i in VS:
        for j in VB:
            variable_label = 'z'+str(i)+'-'+str(j)
            variable_dict[variable_label] = pulp.LpVariable(variable_label, lowBound=0) 

    return problem

# Fairness考慮の有無によらず必要な制約条件を定義
def add_common_constraints(problem,VS,VB,P,Q,pq,shrink_rate,variable_dict):
    problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for j in VB for i in VS)  <= min(P * shrink_rate, Q * shrink_rate)

    for i in VS:
        problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for j in VB) <= pq[i-1] * shrink_rate

    for j in VB:
        problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for i in VS) <= pq[j-1] * shrink_rate

    ## P > Qのとき
    if P>Q:
        for i in VS:
            problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for j in VB) <= Q * shrink_rate

        for j in VB:
            problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for i in VS) <= Q * shrink_rate

    ## Q >=Pのとき
    else:
        for j in VB:
            problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for i in VS) <= P * shrink_rate
        for i in VS:
            problem += pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for j in VB) <= P * shrink_rate

    return problem

# Benefit Rateを求める関数
def calculate_benefit_rate(VS,VB,variable_dict,pq,P,Q):
    benefit_rate_list = []
    result_dict = {}

    # sellerのノードに関して計算
    for i in VS:
        result_dict[i] = 0
        for j in VB:
            variable_label = 'z'+str(i)+'-'+str(j)
            output_value = pulp.value(variable_dict[variable_label])
            result_dict[i] = result_dict[i] + output_value

        benefit_rate = result_dict[i]/min(pq[i-1],Q)
        benefit_rate_list.append(benefit_rate)
        print('Quantity assigned to v'+str(i)+': '+str(result_dict[i]))
        print('  benefit rate:'+str(benefit_rate))
        with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline='') as f:
                writer = csv.writer(f, delimiter='\t')
                writer.writerow('Quantity assigned to v'+str(i)+': '+str(result_dict[i]))
                writer.writerow('  benefit rate:'+str(benefit_rate))

    # buyerのノードに関して計算
    for j in VB:
        result_dict[j] = 0
        for i in VS:
            variable_label = 'z'+str(i)+'-'+str(j)
            output_value = pulp.value(variable_dict[variable_label])
            result_dict[j] = result_dict[j] + output_value

        benefit_rate = result_dict[j]/min(pq[j-1],P)
        benefit_rate_list.append(benefit_rate)
        print('Quantity assigned to v'+str(j)+': '+str(result_dict[j]))
        print('  benefit rate:'+str(benefit_rate)) 
        with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline='') as f:
                writer = csv.writer(f, delimiter='\t')
                writer.writerow('Quantity assigned to v'+str(j)+': '+str(result_dict[j]))
                writer.writerow('  benefit rate:'+str(benefit_rate))

    return benefit_rate_list    

# Fairness Factor (ジニ係数)を求める関数
def gini(input_list):
    # ジニ係数を求める
    gs = inequality.gini.Gini(input_list)
    gini = gs.__dict__['g']
    
    return(gini)

# ソルバー実行結果を表示
def show_solver_result(VS,VB,variable_dict,opt_flag):
    with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline='') as f:
                writer = csv.writer(f, delimiter='\t')
                writer.writerow("--------------------------------")
    # 実行結果の確認
    print('output values:')
    for i in VS:
        for j in VB:
            variable_label = 'z'+str(i)+'-'+str(j)
            print(variable_label, pulp.value(variable_dict[variable_label]))
            insert_value = str(pulp.value(variable_dict[variable_label]))
            with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline='') as f:
                writer = csv.writer(f, delimiter='\t')
                writer.writerow(variable_label)
                writer.writerow(insert_value)
                #writer.writerow(insert_value)
            #df = pd.read_csv("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv")
            #df.at = insert_value
            #df.to_csv("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv")

    # Fairnessも考慮した最適化問題のときは、決定変数wの値も表示
    if opt_flag == True:
        print('w', pulp.value(variable_dict['w']))

# Prosumerが取引を希望する量の算出
def calc_desired_quantity(N,c,d,VS,VB):
    # Prosumerが取引したいと思う量: pq
    pq = [0 for j in range(N)]
    for i in range(N):
        if (i+1) in VS:
            pq[i] = c[i]-d[i]
        elif (i+1) in VB:
            pq[i] = d[i]-c[i]

    print('pq:',pq)
    with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline='') as f:
                writer = csv.writer(f, delimiter='\t')
                writer.writerow('pq:')
                writer.writerow(pq)

    return pq

# Seller集合VSとBuyer集合VBを求める関数
def determine_role_set(N, c, d):
    VS = []
    VB = []
    for i in range(N):
        # capacityの方が大きいなら、roleはseller
        if c[i] > d[i]:
            VS.append(i+1)
        # demandの方が大きいなら、roleはbuyer
        elif d[i] > c[i]:
            VB.append(i+1)

    print("VS:", VS)
    print("VB:", VB)

    return VS, VB

# SellerとBuyerがそれぞれ取引したい量の合計を算出
def calc_total_desired_quantity(VS,VB,pq):
    # 全sellerが取引したい量の合計(P)
    P = 0
    for i in VS:
        P = P + pq[i-1]

    # 全buyerが取引したい量の合計(Q)
    Q = 0
    for i in VB:
        Q = Q + pq[i-1]

    return P,Q

def run_solver(opt_flag,VS,VB,pq,P,Q,shrink_rate):
    # 線形計画問題の決定変数のdictionary(indexに決定変数名が入る)
    variable_dict = {}

    # Fairnessも考慮した最適化問題を実行するときの問題設定
    if opt_flag == True:
        variable_dict, problem = set_problem_opt(VS,VB,variable_dict,pq,P,Q,shrink_rate)
    # Fairnessは考慮しない最適化問題を実行するときの問題設定
    else:
        variable_dict, problem = set_problem(VS,VB,variable_dict,pq,P,Q,shrink_rate)

    # ソルバーの実行
    problem.solve() 

    # ソルバー実行結果を表示
    show_solver_result(VS,VB,variable_dict,opt_flag)

    return variable_dict

# 評価指標算出
def calc_factors(benefit_rate_list,VS,VB,variable_dict,P,Q,fairness_factor_name):
    # Efficiency Rateの算出
    efficiency_rate = pulp.value(pulp.lpSum(variable_dict['z'+str(i)+'-'+str(j)] for j in VB for i in VS))/min(P,Q)
    print('Efficiency Rate:',efficiency_rate)
    with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline = '') as f:
        writer = csv.writer(f, delimiter='\t')
        writer.writerow('Efficiency Rate:')
        writer.writerow(str(efficiency_rate))

    # Fairness Factorの算出(ジニ係数)
    if fairness_factor_name == 'gini':
        fairness_factor_value = gini(benefit_rate_list)
        print('Fairness: Gini coefficient:',fairness_factor_value)
        with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline='') as f:
                writer = csv.writer(f, delimiter='\t')
                writer.writerow('Fairness: Gini coefficient:')
                writer.writerow(str(fairness_factor_value))
    # Fairness Factorの算出(標準偏差)
    else:    
        fairness_factor_value = statistics.stdev(benefit_rate_list)
        print('Fairness: Standard deviation:',fairness_factor_value)
        with open("C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv", "a", newline='') as f:
                writer = csv.writer(f, delimiter='\t')
                writer.writerow('Fairness: Standard deviation:')
                writer.writerow(str(fairness_factor_value))

    return efficiency_rate, fairness_factor_value

# シミュレーション実行
def exec_simulation(N,c,d,shrink_rate,opt_flag,fairness_factor_name):
    # Seller集合VSとBuyer集合VBを求める
    VS, VB = determine_role_set(N, c, d)

    # Prosumerが取引を希望する量(pq)を算出
    pq = calc_desired_quantity(N,c,d,VS,VB)

    # SellerとBuyerがそれぞれ取引したい量の合計を算出
    P,Q = calc_total_desired_quantity(VS,VB,pq)

    # 線形計画問題の設定と実行
    variable_dict = run_solver(opt_flag,VS,VB,pq,P,Q,shrink_rate)

    # benefit_rateの算出
    benefit_rate_list = calculate_benefit_rate(VS,VB,variable_dict,pq,P,Q)

    # 評価指標算出
    efficiency_rate, fairness_factor_value = calc_factors(benefit_rate_list,VS,VB,variable_dict,P,Q,fairness_factor_name)
        
    return efficiency_rate, fairness_factor_value

def calc_factor_results(N,c,d,fairness_factor_name,iter_list):
    # 評価指標の計算結果を格納するdictionary
    factor_result_dict = {}
    factor_result_dict['True'] = {}
    factor_result_dict['False'] = {}

    # Fairness考慮の有無に応じて最適化問題を定義・計算
    for is_fairness_considered in [True,False]:
        efficiency_rate_list = []
        fairness_factor_value_list = []

        # シミュレーション実行（縮小率ごとに実行）
        for i in iter_list:
            shrink_rate = i
            e,f = exec_simulation(N,c,d,shrink_rate,is_fairness_considered,fairness_factor_name)

            # シミュレーションから得た値はリストに格納
            efficiency_rate_list.append(e)
            fairness_factor_value_list.append(f)

        # 実行結果をdictionaryに格納する際のインデックス作成
        if is_fairness_considered == True:
            dict_flag = 'True'
        else:
            dict_flag = 'False'

        # 実行結果をdictionaryに格納
        factor_result_dict[dict_flag]['ER'] = efficiency_rate_list
        factor_result_dict[dict_flag]['FF'] = fairness_factor_value_list

    return factor_result_dict

def output_figure(fairness_factor_name,factor_result_dict, k, c, d):
    #出力画像初期化
    plt.figure()

    # 散布図を描画
    plt.title('Fairness and efficiency')
    plt.xlabel('Efficiency Rate') #x軸の名前

    if fairness_factor_name == 'gini':
        plt.ylabel('Fairness Factor (Gini coefficient)') #y軸の名前
    else:
        plt.ylabel('Fairness Factor (Standard deviation)') #y軸の名前

    plt.ylim(0,1.05) #y軸範囲指定
    plt.xlim(0,1.05) #x軸範囲指定

    #plt.text(0.1, 0.8, "capacity:" + str(c), backgroundcolor="lightblue")
    #plt.text(0.1, 0.7, "demand:" + str(d), backgroundcolor="lightblue")

    # 散布図に系列を追加
    for dict_flag in ['True','False']:
        plt.scatter(factor_result_dict[dict_flag]['ER'], factor_result_dict[dict_flag]['FF'],label='Fairness optimized: '+dict_flag)

    # 判例の表示
    plt.legend()

    # 画像出力(Pythonスクリプトと同フォルダに出力する)
    plt.savefig('fig_'+fairness_factor_name+'_'+str(k)+'.png')

# main関数
if __name__ == '__main__':
    # 定義済みのFairness評価指標ごとに繰り返す
    for k in range(10):
        with open('C:\\Users\\Eiichi Kusatake\\OneDrive\\value.csv', 'w', newline='') as f:
            datawriter = csv.writer(f, delimiter='\t')
            datawriter.writerow('output values:')

        for fairness_factor_name in FAIRNESS_FACTOR_LIST:
        # efficiencyの縮小率のリストを生成(縮小率*min(P,Q)だけ全体として割り当て可能になる)
        # PLOT_NOを10にすると、1系列につき10個プロットする
            iter_list = []
            for i in range(0,PLOT_NO):
                itr_temp = (i+1)/PLOT_NO
                itr = round(itr_temp, 4) #小数点第二位までに絞る
                iter_list.append(itr)

            # 評価指標の計算結果を格納するdictionary
            c = a[k]
            d = b[k]
            factor_result_dict = calc_factor_results(N,c,d,fairness_factor_name,iter_list)

            # 実行結果画像をプロットして出力
            output_figure(fairness_factor_name,factor_result_dict, k, c, d)
